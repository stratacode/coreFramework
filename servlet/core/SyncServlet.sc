
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;

import sc.lang.html.Element;
import sc.lang.SCLanguage;
import sc.lang.java.ModelUtil;
import sc.parser.Language;
import sc.parser.Parselet;
import sc.layer.LayeredSystem;
import sc.dyn.DynUtil;
import sc.util.PerfMon;

import java.io.BufferedReader;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterChain;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import sc.obj.ScopeContext;
import sc.obj.CurrentScopeContext;
import sc.obj.IScopeChangeListener;

import sc.sync.SyncManager;
import sc.sync.RuntimeIOException;
import sc.sync.SyncResult;

import sc.dyn.ScheduledJob;

import sc.js.URLPath;

import sc.db.DBTransaction;

/** 
  * The SyncServlet responds to the sync requests made by the client.  It receives a layer of changes, parses and applies
  * that layer, then gathers up any response data to the sync in the response layer.  That layer gets converted to Java
  * script before it's sent to the client so there's less work on that end.
  */
@PathServlet(path="/sync")
class SyncServlet extends HttpServlet {
   
   public void service(javax.servlet.http.HttpServletRequest request, 
                       javax.servlet.http.HttpServletResponse response) 
                           throws IOException, ServletException {
       if (!handleRequest(request, response)) {
          response.sendError(404, "Page not found");
       }
   }

   public boolean handleRequest(javax.servlet.http.HttpServletRequest request, 
                                javax.servlet.http.HttpServletResponse response) 
                           throws IOException, ServletException {

      /**
       This request contains the POST data to reset the server session based on the client's initial sync.
       This occurs typically after the client has tried to sync and found no session.
       */
      String reset = request.getParameter("reset");
      String closeStr = request.getParameter("close");
      String initStr = request.getParameter("init");
      boolean closeSession = closeStr != null && closeStr.equals("true");
      boolean reInit = initStr != null && initStr.equals("true");

      HttpSession session = request.getSession(false);
      String url = request.getParameter("url");
      String receiveLanguage = request.getParameter("lang"); // Protocol might be a better name here, though I suppose it's still a language?
      if (receiveLanguage == null)
         receiveLanguage = SyncManager.defaultLanguage;
      /** Refresh of true says to do a code-refresh before the sync.  This defaults based on the server default but you can override whether you want to check for code changes on each sync or not. */
      String refresh = request.getParameter("refresh");
      String detailStr = "";

      // If we are not resetting already and this session has not rendered this page, we are in a reset situation - tell the client to send all of the data it has
      if (reset == null && !reInit && (session == null || !PageDispatcher.syncInitialized(session, url))) {
         if (SyncManager.trace)
            Context.logForRequest(request, "sync request received with no sync session in place - sending reset-content response");
         // Send a 205 error - reset content when there's no session for the client to sync again.  Either the session
         // timed out or the server was restarted.   The client can handle that by sync'ing it's data back to the
         // server, to effectively re-create the session.
         response.sendError(HttpServletResponse.SC_RESET_CONTENT, "Session expired for sync - client should do a reset");
         return true;
      }

      long startTime = 0;
      boolean verbosePage = SyncManager.trace || PageDispatcher.trace || PageDispatcher.verbose;
      if (verbosePage)
         startTime = System.currentTimeMillis();

      boolean newSession = false;
      // Resetting the session - need to create it!
      if (session == null) {
         if (closeSession) {
            // This is a weird case - getting the close message after the session has expired
            return true;
         }
         session = request.getSession(true);
         newSession = true;
      }

      // Just mark this session as a sync session for now.  TODO: the client should maybe have a client-id and sequence number
      // so we can recognize out of sequence or conflicting requests for the same session.   That should probably come in as part of the
      // path info or query parameters in the URL.
      // For versioning - how about using the serverTime and clientTime when the sync was initiated.  There will be two time-lines since the clocks are not in skew, but with this approach we also can estimate the latency and time-skew between the
      // two (compute upper and lower bounds for both skew and latency on each for each request.  Refine the upper and lower guestimates and always use the mid-point?)
      PageDispatcher.SyncSession syncSession = PageDispatcher.getSyncSession(session, url, !closeSession);

      if (syncSession == null && closeSession) {
         if (verbosePage)
            System.out.println("Sync: Close session received with no session in place");

         DBTransaction tx = DBTransaction.getCurrent();
         if (tx != null) {
            System.err.println("*** Transaction found on closed sync request");
            tx.rollback();
         }
         return true;
      }

      StringBuilder traceBuffer = new StringBuilder();
      LayeredSystem sys = LayeredSystem.getCurrent();
      PageDispatcher pageDispatcher = PageDispatcher.getPageDispatcher();

      CurrentScopeContext curScopeCtx = null;

      boolean locksAcquired = false;

      SyncWaitListener waitListener = null;
      boolean removeCodeUpdateListener = false;

      boolean enableLog = SyncManager.trace || PageDispatcher.trace;

      Context ctx = null;
      try {
         TreeMap<String,String> queryParams = Context.initQueryParams(request);
         TreeMap<String,Object> urlProps = new TreeMap<String,Object>();
         List<PageDispatcher.PageEntry> pageEnts = pageDispatcher.getPageEntries(url, queryParams, urlProps);
         if (pageEnts == null || pageEnts.size() == 0) {
            try {
               response.sendError(HttpServletResponse.SC_NOT_FOUND, "No page found for url: " + url + " in sync request");
            }
            catch (IOException exc) {}

            DBTransaction tx = DBTransaction.getCurrent();
            if (tx != null) {
               System.err.println("*** Transaction found on not found request");
               tx.rollback();
            }
            return true;
         }

         if (pageEnts == null)
            return true;

         PageDispatcher.PageEntry pageEnt = pageEnts.get(0);

         sc.type.PTypeUtil.setAppId(pageEnt.keyName);

         // This sets up the Context object that wraps the request, response, session and find the existing window scope
         ctx = Context.initContext(pageDispatcher, request, response, null, url, null, true);

         //if (verbosePage)
         //   ctx.log("sync request " + getDebugInfo(request, response));

         int sz = pageEnts.size();

         // Acquires the locks for the context of this page and sets up the list of ScopeContexts (the CurrentScopeContext)
         curScopeCtx = pageDispatcher.initPageContext(ctx, url, pageEnts, session, sys);
         locksAcquired = true;

         String syncGroup = request.getParameter("syncGroup");
         WindowScopeContext windowCtx = ctx.windowCtx;
         boolean closedByServer = false;
         if (windowCtx.windowClosedByServer && !closeSession) {
            if (verbosePage)
               ctx.log("sync request made against window marked as closed: " + getDebugInfo(request, response));
            closedByServer = true;
         }

         SyncWaitListener oldListener = windowCtx.waitingListener;
         // Wake up the previous listener - if any, so there's only one thread per window that's waiting at any given time.
         if (oldListener != null && oldListener.waiting) {
            if (verbosePage) 
               ctx.log("sync request: waking up: " + oldListener.ctx + " " + getDebugInfo(request, response) + (closeSession ? " (session close)" : ""));
            synchronized (oldListener) {
               if (oldListener.waiting) {
                  oldListener.replaced = true;
                  if (closeSession)
                     oldListener.closed = true;
                  oldListener.notify();
               }
            }
         }
         //else if (oldListener != null && verbosePage)
         //   System.out.println("Sync request: not waking up old listener: " + oldListener.threadName + " from: " + ctx + " " + getDebugInfo(request, response) + (closeSession ? " - closing session" : ""));

         if (closeSession) {
            if (verbosePage)
               ctx.log("sync - closing session " + getDebugInfo(request, response));

            try {
               DBTransaction tx = DBTransaction.getCurrent();
               if (tx != null)
                  tx.commit();
            }
            finally {
               // TODO: we could potentially remove the window context here but I think it's good to keep it around for cached back button support. I think the window
               // contexts can be seen as an in-memory record of the navigation structure as well and so useful for the server to understand the
               // browser history. Close session prevents sync listeners from hanging around once the user has navigated away and we remove
               // the scopeContext so that we don't send sync events to this scopeContextName or tie up the command-line interpreter.
               // The browser uses the beacon (or onunload) listener to trigger the closeSession request.
               if (ctx != null && ctx.windowCtx != null) {
                  ctx.windowCtx.removeScopeContext();
               }
            }

            return true;
         }

         if (closedByServer) {
            int resultCode = 410;
            if (!response.isCommitted())
               response.sendError(resultCode, "Session closed by server");
            return true;
         }

         boolean syncApplied = false;
         // When applying a reset or the first request after a session was expired, need to init the page first
         // before applying
         if (reset == null && !newSession) {
            syncApplied = true;
            // Reads the POST data as a layer, applies that layer to the current context, and execs any jobs spawned
            // by the layer.
            applySyncLayer(ctx, request, receiveLanguage, pageEnt, session, url, syncGroup, false);

            DBTransaction tx = DBTransaction.getCurrent();
            if (tx != null)
               tx.commit();
         }

         StringBuilder pageOutput;
         boolean isReset = reset != null;
         StringBuilder reInitSync = null;
         if (url != null) {
            if (reInit)
               reInitSync = new StringBuilder();

            // The applySyncLayer call may have updated the pageEnts applicable for this URL based on changes made to the code as part of the refresh system call.
            // If so, the old ones will be removed and the new ones returned here so this will just update them.
            pageEnts = pageDispatcher.validatePageEntries(pageEnts, url, queryParams, urlProps);

            // Setting initial = isReset || reInitSync here and resetSync = false. -
            // when we are resetting it's the initial sync though we toss this page output, but when it's a reInitSync
            // we return the new init sync output since the initial layer in the client was cleared.
            // In other cases, this call ensures the page is like the client's before we start looking at the sync state
            // to send back.
            // TODO: setting traceBuffer = null here since we never see this output but are there any cases where it might help to debug things?
            pageOutput = pageDispatcher.getPageOutput(ctx, url, pageEnts, urlProps, curScopeCtx,
                                                      isReset || reInit, false, reInitSync, sys, null);
            if (pageOutput == null)
               return true; // Request was redirected, response closed
         }

         // For the reset=true case or first request after a new session, we need to first render the pages from the
         // default initial state, then apply the reset sync from the client.
         if (!syncApplied) {
            applySyncLayer(ctx, request, receiveLanguage, pageEnt, session, url, syncGroup, true);

            pageEnts = pageDispatcher.validatePageEntries(pageEnts, url, queryParams, urlProps);

            // Also render the page after we do the reset so that we lazily init any objects that need synchronizing in this output
            // This time we render with initial = false and resetSync = true - so we do not record any changed made during this page rendering.  We're just resyncing the state of the application to be where the client is already.
            pageOutput = pageDispatcher.getPageOutput(ctx, url, pageEnts, urlProps, curScopeCtx, false, false, null, sys, traceBuffer);
            if (pageOutput == null)
               return true;
         }

         if (reInit) {
            // If the previous session was invalidated from a test script, the client will pass back the scopeContextName param
            // so that we can reassociate the new session with the test script
            if (sys != null && sys.options.testMode) {
               PageDispatcher.updateScopeContext(sys, pageEnt, ctx, request, url);
            }
         }

         SyncManager mgr = SyncManager.getSyncManager("jsHttp");

         int waitTime = -1;
         String waitTimeStr = request.getParameter("waitTime");
         if (waitTimeStr != null) {
            try {
               waitTime = Integer.parseInt(waitTimeStr);
            }
            catch (NumberFormatException exc) {
               System.err.println("*** Invalid waitTime parameter to SyncServlet: " + waitTimeStr);
            }
         }

         if (enableLog && waitTime == -1) {
            detailStr = appendDetail(detailStr, "no wait");
         }

         final SyncWaitListener listener = new SyncWaitListener(ctx);

         if (sys != null && sys.options.autoRefresh) {
            sys.registerCodeUpdateListener(listener);
            removeCodeUpdateListener = true;
            waitListener = listener;
         }

         boolean repeatSync;
         do {
            repeatSync = false;

            CharSequence codeUpdates = null;
            String updateDetailStr = null;
            // TODO: add "code update" as a feature of the sync manager using the 'js' language - move this code into ServletSyncDestination.
            if (syncSession.lastSyncTime != -1 && sys != null && (refresh != null || sys.options.autoRefresh) && !closeSession) {
               codeUpdates = sys.refreshJS(syncSession.lastSyncTime);
               if (enableLog) {
                  if (codeUpdates != null && codeUpdates.length() > 0)
                     updateDetailStr = "*code updates: " + codeUpdates.length();
               }
            }
            SyncResult syncRes;

            ctx.addResponseCookies();

            if (reInitSync != null) {
               // We got the string builder of just the init sync content that's normally included in the page and need
               // to send it to the client here
               syncRes = mgr.sendReInitSync(reInitSync.toString(), syncGroup, pageEnt.pageScope.scopeId, false, codeUpdates);
            }
            else {
               // Now collect up all changes and write them as the response layer.
               syncRes = mgr.sendSync(syncGroup, pageEnt.pageScope.scopeId, false, false, codeUpdates,
                                                 ctx.curScopeCtx.getEventScopeContext().syncTypeFilter,
                                                 ctx.curScopeCtx.getEventScopeContext().resetSyncTypeFilter);
            }

            // If there is nothing to send back to the client now, we have a waitTime supplied, and we do not have to send back the session cookie we can wait for changes for "real time" response to the client
            if ((codeUpdates == null || codeUpdates.length() == 0) && waitTime != -1 && !syncRes.anyChanges &&
                syncRes.errorMessage == null && !newSession && !ctx.cookiesChanged) {
               windowCtx.waitingListener = listener;
               windowCtx.addChangeListener(listener);

               String scopeContextName = (String) windowCtx.getValue("scopeContextName");

               if (locksAcquired) {
                  curScopeCtx.releaseLocks();
                  locksAcquired = false;
               }
               else // TODO: this should not happen right?
                  System.err.println("*** Locks not acquired during sync!");

               boolean interrupted = false;
               long sleepStartTime = 0;
               try {
                  synchronized (listener) {
                     if (!Context.shuttingDown) { // Don't wait if the server is in the midst of shutting down
                        if (verbosePage) {
                           sleepStartTime = System.currentTimeMillis();
                           ctx.log("sync waiting for " + waitTime + " millis " + (scopeContextName == null ? "" : " (scopeContextName: " + scopeContextName + ")") + " " + getDebugInfo(request, response));
                        }

                        if (scopeContextName != null) {
                           // Currently we mark the context as 'ready', i.e. that it's fully initialized the first
                           // time the client receives no more changes from the server - i.e. right before we wait for
                           // the first time.  Maybe there's a need for a more explicit way to control this?  Test
                           // scripts will wait for the context to be ready before they start.  You could imagine that
                           // once the app initializes, it will send some changes to the server which receive replies,
                           // and that can go back and forth for a while before it's really finished initializing.
                           CurrentScopeContext.markReady(scopeContextName, true);
                        }

                        DBTransaction tx = DBTransaction.getCurrent();
                        if (tx != null)
                           tx.commit();

                        // Here's where we wait for some event on the listener or a timeout of waitTime
                        try {
                           listener.waiting = true;
                           listener.wait(waitTime);
                        }
                        catch (InterruptedException exc) {
                           interrupted = true;
                        }
                        finally {
                           listener.waiting = false;
                        }
                     }
                  }
               }
               finally {
                  windowCtx.removeChangeListener(listener);
               }

               ctx.addResponseCookies();

               // Now we've woken up and are ready to look for events

               if (Context.shuttingDown) {
                   if (verbosePage)
                      ctx.log("sync woke - system shutdown: " + (sleepStartTime == 0 ? "" : " after " + (System.currentTimeMillis() - sleepStartTime) + " millis") + " " + getDebugInfo(request, response));
                   // Sending the 410 - resource gone - response here to signal that we do not want the client to poll again.  If we are planning
                   // on restarting, send the 205 - reset which means send all of your data on the next request cause your session is gone
                   int resultCode = Context.restarting ? 205 : 410;
                   if (!response.isCommitted())
                      response.sendError(resultCode, "Session expired for sync - client should do a reset");
                   return true;
               }
               if (windowCtx.windowClosedByServer) {
                  if (verbosePage)
                     ctx.log("sync woke - window closed by server: " + (sleepStartTime == 0 ? "" : " after " + (System.currentTimeMillis() - sleepStartTime) + " millis") + " " + getDebugInfo(request, response));
                  // Sending the 410 - resource gone - response here to signal that we do not want the client to poll again.
                  int resultCode = 410;
                  if (!response.isCommitted())
                     response.sendError(resultCode, "Session closed by server");
                  return true;
               }

               // Make sure we're still the first SyncServlet request waiting...
               if (windowCtx.waitingListener == listener && !listener.replaced) {
                  if (verbosePage)
                     ctx.log("sync woke - acquiring locks: " + " after " + (System.currentTimeMillis() - sleepStartTime) + " millis " + getDebugInfo(request, response));

                  curScopeCtx.startScopeContext(true);

                  // Any jobs queued up for this thread to run get queued up to be run in this thread first (e.g. a refreshTags call
                  // because some server tags were changed)
                  List<ScheduledJob> jobs = curScopeCtx.getEventScopeContext().toRunLater;
                  if (jobs != null) {
                     for (ScheduledJob job:jobs)
                        DynUtil.invokeLater(job.toInvoke, job.priority);
                     jobs.clear();
                  }

                  // This curScopeCtx may have received data binding events from objects it created before we called 'wait'.  When we validate those bindings in startScopeContext, it might have queued additional jobs
                  // that we should perform before we try to do the next sync context.
                  DynUtil.execLaterJobs();

                  locksAcquired = true;

                  // checking again now that we have the locks
                  if (windowCtx.waitingListener == listener && !listener.replaced) {
                     repeatSync = true;
                     if (enableLog) {
                        ctx.log("sync woke: " + " after " + (System.currentTimeMillis() - sleepStartTime) + " millis" +
                                 (interrupted ? " ***interrupted" : "") + detailStr + " " + updateDetailStr + getDebugInfo(request, response));
                     }
                  }
               }
               if (!repeatSync) {
                  if (enableLog) {
                     detailStr = appendDetail(detailStr, "after " + (System.currentTimeMillis() - sleepStartTime) + " millis");
                     if (interrupted)
                        detailStr = appendDetail(detailStr, "***interrupted");
                     detailStr = appendDetail(detailStr, "cancelled");
                     if (closeSession)
                        detailStr = appendDetail(detailStr, "session closed");
                  }
               }
            }
         } while (repeatSync);

         syncSession.lastSyncTime = System.currentTimeMillis();

         if (verbosePage)
            ctx.log("sync end" + detailStr + (traceBuffer.length() > 0 ? (": " + traceBuffer) : "") + " " + getDebugInfo(request, response));
      }
      catch (RuntimeIOException exc) {
         // For the case where the client side just is closed while we are waiting to write.  Only log this as a verbose message for now because it messages up autotests
         // since we are exiting the chrome headless app in mid-sync sometimes
         if (SyncManager.trace || PageDispatcher.trace)
            System.out.println("Sync IO error while sending sync: " + url + ": " + exc.toString());
      }
      catch (RuntimeException exc) {
         if (sys == null) {
            System.err.println("Sync request error: " + ctx + exc);
            exc.printStackTrace();
         }
         else {
            sys.error("Sync request error: " + ctx + exc.toString());
            exc.printStackTrace();
         }
      }
      finally {
         if (removeCodeUpdateListener)
             sys.removeCodeUpdateListener(waitListener);
         try {
            if (ctx != null) {
               if (!locksAcquired && ctx.hasDoLaterJobs()) {
                  if (verbosePage)
                     System.out.println("Reacquiring locks for post-page processing: " + ctx);
                  curScopeCtx.acquireLocks();
                  locksAcquired = true;
               }
               ctx.execLaterJobs();

               DBTransaction tx = DBTransaction.getCurrent();
               if (tx != null)
                  tx.commit();

               ctx.execLaterJobs();

               if (ctx.toInvokeLater != null && ctx.toInvokeLater.size() > 0) {
                  System.err.println("*** Error - Invoke later jobs remain in completed context (0)");
               }

               Context.clearContext();
               sc.type.PTypeUtil.setAppId(null);

               if (ctx.toInvokeLater != null && ctx.toInvokeLater.size() > 0) {
                  System.err.println("*** Error - Invoke later jobs remain in completed context!");
               }

               if (ctx.pageInsts != null) {
                  for (Object pageInst:ctx.pageInsts) {
                     if (pageInst instanceof Element && ((Element) pageInst).refreshTagsScheduled) {
                        System.err.println("*** Error - completed sync with refreshTagsScheduled");
                     }
                  }
               }
            }
         }
         catch (RuntimeException exc) {
            System.err.println("*** Error cleaning up servlet context: " + exc);
            exc.printStackTrace();
         }
         finally {
            if (curScopeCtx != null)
               CurrentScopeContext.popCurrentScopeContext(locksAcquired);
         }
      }
      return true;
   }

   private String appendDetail(String s1, String s2) {
      if (s1.length() == 0)
         return ": " + s2;
      return s1 + ", " + s2;
   }

   private void applySyncLayer(Context ctx, HttpServletRequest request, String receiveLanguage, PageDispatcher.PageEntry pageEnt, HttpSession session, String url, String syncGroup, boolean isReset) throws IOException {
      int len = request.getContentLength();
      boolean noLength = false;
      if (len == -1) {
         noLength = true;
         len = 10000;
      }
      BufferedReader reader = request.getReader();
      char[] buf = new char[len];
      int numRead = reader.read(buf);
      if (!noLength && numRead != len)
         System.err.println("*** Invalid content length");

      String bufStr = new String(buf);

      if (bufStr.length() > 0) {
         if (SyncManager.trace || PageDispatcher.trace) {
            ctx.log("sync request: apply changes from client " + (isReset ? "reset" : "sync") + ":\n" + bufStr + "");
         }

          // Apply changes from the client.
         ServletSyncDestination.applySyncLayer(bufStr, receiveLanguage, pageEnt.pageScope, isReset, "client");
      }
      ctx.execLaterJobs();
   }

   private String getDebugInfo(HttpServletRequest request, HttpServletResponse response) {
      try {
         if (response.isCommitted() && response.getWriter().checkError())
            return "*** response closed ***";
         return "";
      }
      catch (IOException exc) {
         return "response error: " + exc.toString();
      }
   }

   public void log(String mesg) {
      System.err.println("*** Use Context.log instead of the servlet log method");
      super.log(mesg);
   }

}

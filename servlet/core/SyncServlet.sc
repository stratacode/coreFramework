
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

import sc.js.URLPath;

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
      HttpSession session = request.getSession(false);
      String url = request.getParameter("url");
      String receiveLanguage = request.getParameter("lang"); // Protocol might be a better name here, though I suppose it's still a language?
      if (receiveLanguage == null)
         receiveLanguage = SyncManager.defaultLanguage;
      /** Refresh of true says to do a code-refresh before the sync.  This defaults based on the server default but you can override whether you want to check for code changes on each sync or not. */
      String refresh = request.getParameter("refresh");

      // If we are not resetting already and this session has not rendered this page, we are in a reset situation - tell the client to send all of the data it has
      if (reset == null && (session == null || !PageDispatcher.syncInitialized(session, url))) {
         if (SyncManager.trace)
            System.out.println("Sync request received with no sync session in place - sending reset-content response");
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
         session = request.getSession(true);
         newSession = true;
      }

      // Just mark this session as a sync session for now.  TODO: the client should maybe have a client-id and sequence number
      // so we can recognize out of sequence or conflicting requests for the same session.   That should probably come in as part of the
      // path info or query parameters in the URL.
      // For versioning - how about using the serverTime and clientTime when the sync was initiated.  There will be two time-lines since the clocks are not in skew, but with this approach we also can estimate the latency and time-skew between the
      // two (compute upper and lower bounds for both skew and latency on each for each request.  Refine the upper and lower guestimates and always use the mid-point?)
      PageDispatcher.SyncSession syncSession = PageDispatcher.getSyncSession(session, url, true);

      StringBuilder traceBuffer = new StringBuilder();
      LayeredSystem sys = LayeredSystem.getCurrent();
      PageDispatcher pageDispatcher = PageDispatcher.getPageDispatcher();

      CurrentScopeContext curScopeCtx = null;

      boolean locksAcquired = false;

      SyncWaitListener waitListener = null;
      boolean removeCodeUpdateListener = false;

      Context ctx = null;
      try {
         TreeMap<String,String> queryParams = Context.initQueryParams(request);
         List<PageDispatcher.PageEntry> pageEnts = pageDispatcher.getPageEntries(url, queryParams);
         if (pageEnts == null || pageEnts.size() == 0) {
            try {
               response.sendError(HttpServletResponse.SC_NOT_FOUND, "No page found for url: " + url + " in sync request");
            }
            catch (IOException exc) {}
            return true;
         }

         if (pageEnts == null)
            return true;

         PageDispatcher.PageEntry pageEnt = pageEnts.get(0);

         sc.type.PTypeUtil.setAppId(pageEnt.keyName);

         // This sets up the Context object that wraps the request, response, session and find the existing window scope
         ctx = Context.initContext(request, response, null);

         if (verbosePage) 
            System.out.println("Sync request: " + url + PageDispatcher.getTraceInfo(session) + " " + getDebugInfo(request, response));

         int sz = pageEnts.size();

         // Acquires the locks for the context of this page
         curScopeCtx = pageDispatcher.initPageContext(ctx, url, pageEnts, session, sys);

         String syncGroup = request.getParameter("syncGroup");
         WindowScopeContext windowCtx = ctx.windowCtx;

         SyncWaitListener oldListener = windowCtx.waitingListener;
         // Wake up the previous listener - if any, so there's only one thread per window that's waiting at any given time.
         if (oldListener != null && oldListener.waiting) {
            if (verbosePage) 
                System.out.println("Sync - waking up thread: " + oldListener.threadName + " from: " + PageDispatcher.getTraceInfo(session) + " " + getDebugInfo(request, response));
            synchronized (oldListener) {
               if (oldListener.waiting) {
                  oldListener.replaced = true;
                  oldListener.notify();
               }
            }
         }
         else if (oldListener != null && verbosePage)
            System.out.println("Sync - not waking up thread: " + oldListener.threadName + " from: " + PageDispatcher.getTraceInfo(session) + " " + getDebugInfo(request, response));

         if (reset == null) {
            // Reads the POST data as a layer, applies that layer to the current context, and execs any jobs spawned
            // by the layer.
            applySyncLayer(ctx, request, receiveLanguage, session, url, syncGroup, false);
         }

         StringBuilder pageOutput;
         boolean isReset = reset != null;
         if (url != null) {

            // Setting initial = isReset here and resetSync = false. - when we are resetting it's the initial sync though we toss this page output.  It just sets up the page to be like the client's state when it's first page was shipped out.
            // TODO: setting traceBuffer = null here since we never see this output but are there any cases where it might help to debug things?
            pageOutput = pageDispatcher.getPageOutput(ctx, url, pageEnts, curScopeCtx, isReset, false, sys, null);
            if (pageOutput == null)
               return true;
            locksAcquired = true;
         }

         // For the reset=true case, we need to first render the pages from the default initial state, then apply
         // the reset sync from the client.
         if (reset != null) {
            applySyncLayer(ctx, request, receiveLanguage, session, url, syncGroup, true);

            // Also render the page after we do the reset so that we lazily init any objects that need synchronizing in this output
            // This time we render with initial = false and resetSync = true - so we do not record any changed made during this page rendering.  We're just resyncing the state of the application to be where the client is already.
            pageOutput = pageDispatcher.getPageOutput(ctx, url, pageEnts, curScopeCtx, false, false, sys, traceBuffer);
            if (pageOutput == null)
               return true;
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
            // TODO: add "code update" as a feature of the sync manager using the 'js' language - move this code into ServletSyncDestination.
            if (syncSession.lastSyncTime != -1 && sys != null && (refresh != null || sys.options.autoRefresh)) {
               codeUpdates = sys.refreshJS(syncSession.lastSyncTime);
            }

            // Now collect up all changes and write them as the response layer.  TODO: should this be request scoped?
            SyncResult syncRes = mgr.sendSync(syncGroup, WindowScopeDefinition.scopeId, false, codeUpdates, ctx.curScopeCtx.syncTypeFilter);

            // If there is nothing to send back to the client now, we have a waitTime supplied, and we do not have to send back the session cookie we can wait for changes for "real time" response to the client
            if ((codeUpdates == null || codeUpdates.length() == 0) && waitTime != -1 && !syncRes.anyChanges && syncRes.errorMessage == null && !newSession) {
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
                           System.out.println("Sync wait: " + url + (scopeContextName == null ? "" : " (scopeContextName: " + scopeContextName + ")") + " time: " + waitTime + PageDispatcher.getTraceInfo(session) + " " + getDebugInfo(request, response));
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

               if (Context.shuttingDown) {
                   if (verbosePage)
                      System.out.println("Sync woke - shutdown: " + url + PageDispatcher.getTraceInfo(session) + (sleepStartTime == 0 ? "" : " after " + (System.currentTimeMillis() - sleepStartTime) + " millis") + " " + getDebugInfo(request, response));
                   // Sending the 410 - resource gone - response here to signal that we do not want the client to poll again.  If we are planning
                   // on restarting, send the 205 - reset which means send all of your data on the next request cause your session is gone
                   int resultCode = Context.restarting ? 205 : 410;
                   response.sendError(resultCode, "Session expired for sync - client should do a reset");
                   return true;
               }

               // Make sure we're still the first SyncServlet request waiting...
               if (windowCtx.waitingListener == listener && !listener.replaced) {
                  if (verbosePage)
                     System.out.println("Sync woke - acquiring locks: " + url + PageDispatcher.getTraceInfo(session) + " after " + (System.currentTimeMillis() - sleepStartTime) + " millis " + getDebugInfo(request, response));

                  curScopeCtx.startScopeContext(true);

                  // This curScopeCtx may have received data binding events from objects it created before we called 'wait'.  When we validate those bindings in startScopeContext, it might have queued additional jobs
                  // that we should perform before we try to do the next sync context.
                  DynUtil.execLaterJobs();

                  locksAcquired = true;

                  // checking again now that we have the locks
                  if (windowCtx.waitingListener == listener && !listener.replaced) {
                     repeatSync = true;
                     if (SyncManager.trace || PageDispatcher.trace)
                        System.out.println("Sync woke: " + url + PageDispatcher.getTraceInfo(session) + " after " + (System.currentTimeMillis() - sleepStartTime) + " millis" + (interrupted ? " *** interrupted" : "") + " " + getDebugInfo(request, response));
                  }
               }
               if (!repeatSync) {
                  if (SyncManager.trace || PageDispatcher.trace)
                     System.out.println("Sync woke: " + url +  PageDispatcher.getTraceInfo(session) + " after " + (System.currentTimeMillis() - sleepStartTime) + " millis"  + (interrupted ? " *** interrupted and" : "") + " replaced - returning empty sync: " + " " + getDebugInfo(request, response));
               }
            }
         } while (repeatSync);

         syncSession.lastSyncTime = System.currentTimeMillis();

         if (verbosePage)
            System.out.println("Sync end: " + url + PageDispatcher.getTraceInfo(session) + (traceBuffer.length() > 0 ? (": " + traceBuffer) : "") + " for " + PageDispatcher.getRuntimeString(startTime) + " " + getDebugInfo(request, response));
      }
      catch (RuntimeIOException exc) {
         // For the case where the client side just is closed while we are waiting to write.  Only log this as a verbose message for now because it messages up autotests
         // since we are exiting the chrome headless app in mid-sync sometimes
         if (SyncManager.trace || PageDispatcher.trace)
            System.out.println("Sync IO error while sending sync: " + url + ": " + exc.toString());
      }
      catch (RuntimeException exc) {
         if (sys == null) {
            System.err.println("Sync request error: " + url + PageDispatcher.getTraceInfo(session) + exc);
            exc.printStackTrace();
         }
         else {
            sys.error("Sync request error: " + url + PageDispatcher.getTraceInfo(session) + exc.toString());
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
                     System.out.println("Reacquiring locks for post-page processing: " + PageDispatcher.getTraceInfo(session));
                  curScopeCtx.acquireLocks();
                  locksAcquired = true;
               }
               ctx.execLaterJobs();
               Context.clearContext();
               sc.type.PTypeUtil.setAppId(null);
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

   private void applySyncLayer(Context ctx, HttpServletRequest request, String receiveLanguage, HttpSession session, String url, String syncGroup, boolean isReset) throws IOException {
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
            System.out.println("Client " + (isReset ? "reset" : "sync") + ": " + url + PageDispatcher.getTraceInfo(session) + ":\n" + bufStr + "");
         }

          // Apply changes from the client.
         ServletSyncDestination.applySyncLayer(bufStr, receiveLanguage, isReset, "client");
      }
      ctx.execLaterJobs();
   }

   private String getDebugInfo(HttpServletRequest request, HttpServletResponse response) {
      try {
         return "response closed: " + response.getWriter().checkError();
      }
      catch (IOException exc) {
         return "response error: " + exc.toString();
      }
   }
}


import java.util.LinkedHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;

import sc.lang.html.Element;
import sc.lang.SCLanguage;
import sc.lang.pattern.Pattern;
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

import sc.obj.ScopeEnvironment;
import sc.sync.SyncManager;
import sc.sync.RuntimeIOException;
import sc.sync.SyncResult;

import sc.obj.IScopeChangeListener;

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
      String receiveLanguage = request.getParameter("lang");
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
      if (SyncManager.trace || PageDispatcher.trace)
         startTime = System.currentTimeMillis();

      // Resetting the session - need to create it!
      if (session == null)
         session = request.getSession(true);

      // Just mark this session as a sync session for now.  TODO: the client should maybe have a client-id and sequence number
      // so we can recognize out of sequence or conflicting requests for the same session.   That should probably come in as part of the
      // path info or query parameters in the URL.
      // For versioning - how about using the serverTime and clientTime when the sync was initiated.  There will be two time-lines since the clocks are not in skew, but with this approach we also can estimate the latency and time-skew between the
      // two (compute upper and lower bounds for both skew and latency on each for each request.  Refine the upper and lower guestimates and always use the mid-point?)
      PageDispatcher.SyncSession syncSession = PageDispatcher.getSyncSession(session, url, true);

      ArrayList<Lock> locks = new ArrayList<Lock>();
      StringBuilder traceBuffer = new StringBuilder();
      LayeredSystem sys = LayeredSystem.getCurrent();

      boolean locksAcquired = false;

      Context ctx = null;
      try {
         if (url != null) {
            ScopeEnvironment.setAppId(url);
         }

         ctx = Context.initContext(request, response);

         String syncGroup = request.getParameter("syncGroup");
         if (reset == null) {
            // Reads the POST data as a layer, applies that layer to the current context, and execs any jobs spawned
            // by the layer.
            applySyncLayer(ctx, request, receiveLanguage, session, url, syncGroup, false);
         }

         PageDispatcher pageDispatcher = PageDispatcher.getPageDispatcher();
         StringBuilder pageOutput;
         boolean isReset = reset != null;
         if (url != null) {

            // Setting initial = isReset here and resetSync = false. - when we are resetting it's the initial sync though we toss this page output.  It just sets up the page to be like the client's state when it's first page was shipped out.
            // TODO: setting traceBuffer = null here since we never see this output but are there any cases where it might help to debug things?
            pageOutput = pageDispatcher.getPageOutput(ctx, url, isReset, false, locks, sys, null);
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
            pageOutput = pageDispatcher.getPageOutput(ctx, url, false, false, null, sys, traceBuffer);
            if (pageOutput == null)
               return true;
         }

         SyncManager mgr = SyncManager.getSyncManager("jsHttp");
         CharSequence codeUpdates = null;
         // TODO: add "code update" as a feature of the sync manager using the 'js' language - move this code into ServletSyncDestination.
         if (syncSession.lastSyncTime != -1 && sys != null && (refresh != null || sys.options.autoRefresh)) {
            codeUpdates = sys.refreshJS(syncSession.lastSyncTime);
         }

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

         boolean repeatSync;
         do {
            repeatSync = false;
            // If another request is waiting, let them know we are handling the next sync
            ctx.windowCtx.waitingContext = null;

            // Now collect up all changes and write them as the response layer.  TODO: should this be request?
            SyncResult syncRes = mgr.sendSync(syncGroup, WindowScopeDefinition.scopeId, false, codeUpdates);

            // If there is nothing to send back to the client now and we have a waitTime supplied, we can wait for changes for "real time" response to the client
            if ((codeUpdates == null || codeUpdates.length() == 0) && waitTime != -1 && !syncRes.anyChanges && syncRes.errorMessage == null) {
               ctx.windowCtx.waitingContext = ctx;

               if (locksAcquired) {
                  PageDispatcher.releaseLocks(locks, session);
                  locksAcquired = false;
               }
               else // TODO: this should not happen right?
                  System.err.println("*** Locks not acquired during sync!");

               final IScopeChangeListener listener = new IScopeChangeListener() {
                  synchronized void scopeChanged() {
                     this.notify();
                  }
               };

               boolean interrupted = false;
               long sleepStartTime = 0;
               try {
                  ctx.windowCtx.addChangeListener(listener);

                  synchronized (listener) {
                     if (SyncManager.trace || PageDispatcher.trace) {
                        sleepStartTime = System.currentTimeMillis();
                        System.out.println("Sync servlet waiting: " + waitTime + PageDispatcher.getTraceInfo(session));
                     }
                     try {
                        listener.wait(waitTime);
                     }
                     catch (InterruptedException exc) {
                        interrupted = true;
                     }
                  }
               }
               finally {
                  ctx.windowCtx.removeChangeListener(listener);
               }

               // Make sure we're still the first SyncServlet request waiting...
               if (ctx.windowCtx.waitingContext == ctx) {
                  PageDispatcher.acquireLocks(locks, url);
                  locksAcquired = true;

                  // checking again now that we have the locks
                  if (ctx.windowCtx.waitingContext == ctx) {
                     repeatSync = true;
                     if (SyncManager.trace || PageDispatcher.trace)
                        System.out.println("Sync servlet awoke - resyncing on thread: " + PageDispatcher.getTraceInfo(session) + " after " + (System.currentTimeMillis() - sleepStartTime) + " millis (interrupted: " + interrupted + ")");
                  }
               }
               if (!repeatSync) {
                  if (SyncManager.trace || PageDispatcher.trace)
                     System.out.println("Sync servlet awoke - replaced by another request, returning empty sync: " + PageDispatcher.getTraceInfo(session) + " after " + (System.currentTimeMillis() - sleepStartTime) + " millis (interrupted: " + interrupted + ")");
               }
            }
         } while (repeatSync);

         syncSession.lastSyncTime = System.currentTimeMillis();

         if (SyncManager.trace || PageDispatcher.trace)
            System.out.println("Sync complete:" + PageDispatcher.getTraceInfo(session) + ": " + traceBuffer + ": " + PageDispatcher.getRuntimeString(startTime));
      }
      catch (RuntimeIOException exc) {
         // For the case where the client side just is closed while we are waiting to write.  Only log this as a verbose message for now because it messages up autotests
         // since we are exiting the chrome headless app in mid-sync sometimes
         if (SyncManager.trace || PageDispatcher.trace)
            System.out.println("Sync IO error while sending sync: " + exc.toString());
      }
      catch (RuntimeException exc) {
         if (sys == null) {
            System.err.println("Sync request error: " + exc);
            exc.printStackTrace();
         }
         else {
            sys.error("Sync request error: " + exc.toString());
            exc.printStackTrace();
         }
      }
      finally {
         try {
            if (ctx != null) {
               if (!locksAcquired && ctx.hasDoLaterJobs()) {
                  PageDispatcher.acquireLocks(locks, url);
                  locksAcquired = true;
               }
               ctx.execLaterJobs();
               Context.clearContext();
               ScopeEnvironment.setAppId(null);
            }
         }
         catch (RuntimeException exc) {
            System.err.println("*** Error cleaning up servlet context: " + exc);
            exc.printStackTrace();
         }
         finally {
            if (locksAcquired)
               PageDispatcher.releaseLocks(locks, session);
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

      if (SyncManager.trace || PageDispatcher.trace) {
         // TODO: add session id, timestamp.
         System.out.println("Received sync from client: " + (isReset ? "reset" : "sync") + PageDispatcher.getTraceInfo(session) + ":\n" + bufStr + "");
      }

       // Apply changes from the client.
      ServletSyncDestination.applySyncLayer(bufStr, receiveLanguage, isReset);

      ctx.execLaterJobs();

   }
}

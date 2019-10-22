import java.util.Set;
import java.io.PrintWriter;
import sc.sync.SyncManager;

import sc.dyn.DynUtil;
import sc.dyn.ScheduledJob;
import sc.dyn.IScheduler;
import sc.obj.ScopeDefinition;
import sc.obj.ScopeContext;
import sc.obj.RequestScopeDefinition;
import sc.obj.CurrentScopeContext;

import sc.type.PTypeUtil;
import sc.lang.html.Window;

import java.util.Enumeration;
import java.util.TreeMap;

import sc.sync.RuntimeIOException;

/** 
  * An abstraction around the servlet request and response, stored in thread-local so any code running
  * in the context of the request can get access to per-request, per-session and per-window data, run
  * 'do later' jobs, set the output type and write to the response.
  *
  * TODO: we could break this down into a platform independent part and provide a seamless way for servlet
  * apps to move to other runtime environments that follow the request/response model.
  */
class Context {
   HttpServletRequest request;
   HttpServletResponse response;
   HttpSession session;
   String mimeType;
   boolean mimeTypeSet = false;
   boolean requestComplete;
   boolean windowRequest = true; // When processing a session invalidate event, we are not from a window

   TreeMap<String,String> queryParams;

   ArrayList<ScheduledJob> toInvokeLater = null;

   WindowScopeContext windowCtx = null;

   CurrentScopeContext curScopeCtx = null;

   String requestURL;
   String requestURI;

   /** Set to true when the server is in the midst of a shutdown */
   static boolean shuttingDown = false;
   /** Set this to true when the server will restart */
   static boolean restarting = false;

   static boolean verbose = false;
   static boolean trace = false;
   static boolean testMode = false;

   Context(HttpServletRequest req, HttpServletResponse res, String requestURL, String requestURI, TreeMap<String,String> queryParams) {
      request = req;
      response = res;
      this.requestURL = requestURL;
      this.requestURI = requestURI;
      this.queryParams = queryParams;
   }

   /** Use this in session destruction hook, when request/response is not available */
   Context(HttpSession session) {
      this.session = session;
   }

   HttpSession getSession() {
      if (request == null)
         return session;
      // TODO: performance - maybe add: if (session !=null) return session;
      try {
         if (session != null)
            return session;
         HttpSession sess = request.getSession(true);
         session = sess;
         return sess;
      }
      catch (IllegalStateException exc) {
         if (verbose)
            System.out.println("Response is committed error trying to get session");
      }
      return null;
   }

   private static ThreadLocal<Context> currentContextStore = new ThreadLocal<Context>();

   static Context getCurrentContext() {
      return currentContextStore.get();
   }

   static HttpSession getCurrentSession() {
      Context ctx = getCurrentContext();
      if (ctx == null)
         return null;
      HttpSession session = ctx.getSession();
      return session;
   }

   WindowScopeContext initWindowScopeContext(int windowId) {
      HttpSession session = getSession();
      if (session != null) {
         ArrayList<WindowScopeContext> ctxList = (ArrayList<WindowScopeContext>) session.getAttribute("_windowContexts");
         if (ctxList != null) {
            synchronized (ctxList) {
               for (int i = 0; i < ctxList.size(); i++) {
                  WindowScopeContext winCtx = ctxList.get(i);
                  if (winCtx == null) {
                     System.err.println("*** Invalid null window context");
                     continue;
                  }
                  if (winCtx.windowId == windowId) {
                     updateWindowContext(winCtx);
                     return windowCtx;
                  }
               }
            }
         }
      }
      windowCtx = getWindowScopeContext(true);
      windowCtx.windowId = windowId;
      updateWindowContext(windowCtx);
      if (verbose && session != null)
         System.out.println("Request for existing window: " + windowId + " session id: " + session.getId() + "/" + DynUtil.getTraceObjId(session.getId()) + " on thread: " + DynUtil.getCurrentThreadString());
      return windowCtx;
   }

   void updateWindowContext(WindowScopeContext winCtx) {
      windowCtx = winCtx;
      if (winCtx != null) {
         winCtx.lastRequestTime = System.currentTimeMillis();
         PTypeUtil.setWindowId(winCtx.windowId);
      }
      else
         PTypeUtil.clearThreadLocal("windowId");
   }

   /** Used for session shutdown or other access of session where the request/response are not present */
   static Context initContext(HttpSession session) {
      Context ctx;
      currentContextStore.set(ctx = new Context(session));
      ctx.windowRequest = false;
      return ctx;
   }

   static Context initContext(HttpServletRequest request, HttpServletResponse response, String requestURL, String requestURI, TreeMap<String,String> queryParams) {
      Context ctx;
      currentContextStore.set(ctx = new Context(request, response, requestURL, requestURI, queryParams));

      String windowIdStr = request.getParameter("windowId");
      if (windowIdStr != null) {
         int windowId = Integer.parseInt(windowIdStr);
         try {
            ctx.initWindowScopeContext(windowId);
         }
         catch (NumberFormatException exc) {
         }
      }
      else {
         ctx.updateWindowContext(ctx.getWindowScopeContext(true));
      }
      // TODO: populate this with data from the request/response - url and compute a size from the device meta-data
      // You might want to render different content based on the device size for example so that would be nice to have here.
      // Of course the location for rendering links the same between client and server
      Window.setWindow(ctx.windowCtx.getWindow());
      return ctx;
   }

   static void clearContext() {
      currentContextStore.set(null);
      ScopeContext requestCtx = RequestScopeDefinition.getRequestScopeDefinition().getScopeContext(false);
      if (requestCtx != null) {
         requestCtx.scopeDestroyed(null);
      }
   }


   void execLaterJobs() {
      execLaterJobs(IScheduler.NO_MIN, IScheduler.NO_MAX);
   }

   void execLaterJobs(int minPriority, int maxPriority) {
      if (toInvokeLater != null) {
         SyncManager.SyncState origState = null;
         ArrayList<ScheduledJob> toRestore = null;
         try {
            origState = SyncManager.getSyncState();
            // While running any callbacks, we are in the recording state, even if invoking these as part of the
            // initialization phase.  This is really like Initializing but where there's a nested binding count.
            SyncManager.setSyncState(SyncManager.SyncState.RecordingChanges);

            while (toInvokeLater != null) {
               ArrayList<ScheduledJob> toRun = (ArrayList<ScheduledJob>)toInvokeLater.clone();
               // Zero this out here so we start accumulating a new list and keep processing this list until
               // we have no more work to do later.
               toInvokeLater = null;
               for (ScheduledJob sj:toRun) {
                  if (sj.priority > minPriority && sj.priority < maxPriority)
                     sj.run();
                  else {
                     if (toRestore == null)
                        toRestore = new ArrayList<ScheduledJob>();
                     toRestore.add(sj);
                  }
               }
            }
            if (toRestore != null) {
               if (toInvokeLater == null)
                  toInvokeLater = toRestore;
               else
                  toInvokeLater.addAll(toRestore);
            }
         }
         finally {
            SyncManager.setSyncState(origState);
         }
      }
   }

   public void setWindowId(int windowId) {
      windowCtx.windowId = windowId;
   }

   public int getWindowId() {
      return getWindowScopeContext(true).windowId;
   }

   public static boolean getHasWindowScope() {
      return getWindowScope(false) != null;
   }

   public static WindowScopeContext getWindowScope(boolean create) {
      Context current = getCurrentContext();
      if (current == null)
         return null;
      return current.getWindowScopeContext(create);
   }

   public void destroyWindowScopes() {
      HttpSession session = getSession();
      if (session == null)
         return;
      ArrayList<WindowScopeContext> ctxList = (ArrayList<WindowScopeContext>) session.getAttribute("_windowContexts");
      if (ctxList != null) {
         for (WindowScopeContext winScope:ctxList) {
            winScope.scopeDestroyed(null);
            winScope.removeScopeContext();
         }
         ctxList.clear();
         session.removeAttribute("_windowContexts");
      }
   }

   public static void destroyContext(HttpSession session) {
      Context ctx = Context.initContext(session);

      if (verbose)
         System.out.println("Session invalidated: " + ctx.getTraceInfo());

      // Now do the attributes in the session
      try {
         // Destroy the window scopes first since they are up-stream of the session
         ctx.destroyWindowScopes();

         Enumeration<String> attNames = session.getAttributeNames();
         // Copy this in case something in the dispose process adds a new element
         ArrayList<String> attNameList = new ArrayList<String>();
         while (attNames.hasMoreElements()) {
            attNameList.add(attNames.nextElement());
         }
         for (String str:attNameList) {
            Object value = session.getAttribute(str);
            if (value != null) {
               DynUtil.dispose(value);
            }
         }
         // This eventually calls the sync destroy listener which will remove any remaining sync instances.  If we do this
         // before the attributes above, we dipose objects twice
         SessionScopeContext scopeCtx = (SessionScopeContext) session.getAttribute("_sessionScopeContext");
         if (scopeCtx != null)
            scopeCtx.scopeDestroyed(null);
      }
      finally {
         Context.clearContext();
      }
   }

   public WindowScopeContext getWindowScopeContext(boolean create) {
      if (windowCtx == null && windowRequest) {
         HttpSession session = getSession();
         if (session == null)
            return null;
         ArrayList<WindowScopeContext> ctxList = (ArrayList<WindowScopeContext>) session.getAttribute("_windowContexts");
         int windowId;
         if (ctxList == null) {
            if (!create)
               return null;
            // TODO: is it safe to sync on the session?  This may conflict with locks in the servlet implementation itself
            synchronized (session) {
               ctxList = (ArrayList<WindowScopeContext>) session.getAttribute("_windowContexts");
               if (ctxList == null) {
                  ctxList = new ArrayList<WindowScopeContext>();
                  session.setAttribute("_windowContexts", ctxList);
               }
            }
         }
         synchronized (ctxList) {
            Integer nextWindowId = (Integer) session.getAttribute("_nextWindowId");
            if (nextWindowId == null)
               nextWindowId = 0;
            session.setAttribute("_nextWindowId", nextWindowId+1);

            if (!testMode) {
               String sessionId = session.getId();
               // Include some integer based on the session id in the windowId to make it resilient to restarts.
               // Although windowId only has to be unique within a given session and is not used for authentication access - that's
               // done at the session level, if a server restarts, an old tab with windowId=0 would get confused with a new one
               int sessionPart = 0;
               for (int i = 0; i < sessionId.length(); i++)
                  sessionPart += sessionId.charAt(i);
               sessionPart = sessionPart % 1000000 * 100;
               windowId = sessionPart + nextWindowId;
            }
            else
               windowId = nextWindowId;
            String queryStr = request.getQueryString();
            String fullURL = queryParams == null ? requestURL : requestURL + "?" + queryStr;
            windowCtx = new WindowScopeContext(windowId, Window.createNewWindow(fullURL, request.getServerName(), request.getServerPort(), request.getRequestURI(), request.getPathInfo(), queryStr));
            windowCtx.init();
            windowCtx.lastRequestTime = System.currentTimeMillis();
            ctxList.add(windowCtx);

            SyncManager.addSyncInst(windowCtx.window, true, false, "window", null);

            if (verbose) {
               System.out.println("New window: " + windowId + " session id:" + session.getId() + "/" + DynUtil.getTraceObjId(session.getId()) + " thread: " + DynUtil.getCurrentThreadString());
            }

            // First we look for the first non-waiting window to remove. Then we just stop the waiter and remove it anyway.
            if (!enforceMaxWindows(ctxList, false))
               enforceMaxWindows(ctxList, true);
         }
         PTypeUtil.setWindowId(windowId);
      }
      return windowCtx;
   }

   // Removes the oldest window in the session. If stopWaitingWindows is false, we skip windows where there's
   // still a waiting thread since it's more likely they will be active still in case there are multiple tabs.
   // Once we hit the max though and
   private boolean enforceMaxWindows(ArrayList<WindowScopeContext> ctxList, boolean stopWaitingWindows) {
      int numCtxs = ctxList.size();
      WindowScopeContext toRem = null;
      int toRemIx = -1;
      if (numCtxs > WindowScopeDefinition.maxWindowsPerSession) {
         // Look through the window scope contexts and find the least recently used one that does not have a sync request waiting
         for (int i = 0; i < numCtxs; i++) {
            WindowScopeContext wsc = ctxList.get(i);
            SyncWaitListener syncListener = wsc.waitingListener;
            if (stopWaitingWindows || syncListener == null || !syncListener.waiting) {
               if (toRem == null || (wsc.lastRequestTime == -1 || (toRem.lastRequestTime > wsc.lastRequestTime))) {
                  toRem = wsc;
                  toRemIx = i;
                  if (toRem.lastRequestTime == -1)
                     break;
               }
            }
         }
      }
      if (toRem != null) {
         toRem.scopeDestroyed(null);
         if (verbose)
            System.out.println("Removing window scope: " + windowId + " for: " + toRem.getTraceId() + " - exceeded max windows per session: " + numCtxs);
         ctxList.remove(toRemIx);

         SyncWaitListener syncListener = toRem.waitingListener;
         if (syncListener != null && syncListener.waiting) {
            syncListener.closed = true;
            syncListener.notify();
         }
         return true;
      }
      return false;
   }

   void write(String str) {
      PrintWriter writer;
      if (mimeType != null && !mimeTypeSet) {
         response.setContentType(mimeType);
         mimeTypeSet = true;
      }
      try {
         writer = response.getWriter();
         if (writer.checkError())
            throw new RuntimeIOException("response already closed on write");
         writer.print(str);
         writer.flush();
      }
      // Jetty throws org.eclipse.jetty.io.RuntimeIOException but we don't want to burn in a dependency here
      catch (RuntimeException ioexc) {
         if (ioexc.getClass().getName().contains("RuntimeIOException"))
            throw new RuntimeIOException(ioexc.toString());
      }
      catch (IOException exc) {
         throw new IllegalArgumentException("failed to write to client: " + exc.toString());
      }
   }

   void invokeLater(ScheduledJob sj) {
      if (toInvokeLater == null)
         toInvokeLater = new ArrayList<ScheduledJob>();
      ScheduledJob.addToJobList(toInvokeLater, sj);
   }

   boolean clearInvokeLater(ScheduledJob sj) {
      if (toInvokeLater == null)
         return false;
      return ScheduledJob.removeJobFromList(toInvokeLater, sj);
   }

   boolean hasDoLaterJobs() {
      return toInvokeLater != null && toInvokeLater.size() > 0;
   }

   String getTraceInfo() {
      if (verbose || trace)
          return " session: " + DynUtil.getTraceObjId(session.getId()) + " thread: " + DynUtil.getCurrentThreadString();
      return null;
   }

   static TreeMap<String,String> initQueryParams(HttpServletRequest request) {
      TreeMap<String,String> queryParams = null;
      String queryString = request.getQueryString();
      return getQueryParamsFromQueryString(queryString);
   }

   static TreeMap<String,String> getQueryParamsFromQueryString(String queryString) {
      if (queryString == null)
         return null;
      TreeMap<String,String> queryParams = null;
      String[] paramStrs = queryString.split("&");
      queryParams = new TreeMap<String,String>();
      for (String paramStr:paramStrs) {
         int ix = paramStr.indexOf("=");
         if (ix == -1) {
            queryParams.put(paramStr, "");
         }
         else
            queryParams.put(paramStr.substring(0,ix), paramStr.substring(ix+1));
      }
      return queryParams;
   }

   String getQueryParam(String queryParam) {
      if (queryParams == null) {
         if (request != null) {
            queryParams = initQueryParams(request);
         }
         else
            return null; // Should we have grabbed the query params before now?
      }
      return queryParams.get(queryParam);
   }

   String toString() {
      StringBuilder sb = new StringBuilder();
      if (requestURI != null) {
         sb.append("url:");
         sb.append(requestURI);
         sb.append(" ");
      }
      sb.append("thread:" + DynUtil.getCurrentThreadString());
      if (windowCtx != null) {
         sb.append(" ");
         sb.append(windowCtx);
      }
      if (session != null) {
         sb.append(" session:");
         sb.append(DynUtil.getTraceId(session.getId()));
      }
      return sb.toString();
   }
}

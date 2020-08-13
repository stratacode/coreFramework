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
import sc.util.PerfMon;

import sc.type.PTypeUtil;
import sc.lang.html.Window;

import java.util.Enumeration;
import java.util.TreeMap;

import sc.sync.RuntimeIOException;
import javax.servlet.http.Cookie;

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
   boolean needsInvalidate = false; // Set to true to invalidate the session after the request

   TreeMap<String,String> queryParams;

   ArrayList<ScheduledJob> toInvokeLater = null;

   WindowScopeContext windowCtx = null;

   CurrentScopeContext curScopeCtx = null;

   TreeMap<String,Cookie> cookieMap = null;

   // For diagnostics only
   String threadName = DynUtil.getCurrentThreadString();

   /** The top-level pageInstance(s) for diagnostics */
   List<Object> pageInsts;

   String requestURL;
   String requestURI;

   String redirectURL;
   String requestError;

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
            log("response is committed error trying to get session");
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
         log("cached window");
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

   static Context initContext(HttpServletRequest request, HttpServletResponse response, String requestURL, String requestURI, TreeMap<String,String> queryParams, boolean isDynPage) {
      Context ctx;
      if (requestURI == null)
         requestURI = request.getRequestURI();
      if (requestURL == null)
         requestURL = buildRequestURL(request, requestURI);

      currentContextStore.set(ctx = new Context(request, response, requestURL, requestURI, queryParams));

      if (isDynPage) {
         response.addHeader("Cache-Control", "no-store");

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
      }
      return ctx;
   }

   static void clearContext() {
      Context current = getCurrentContext();
      if (current != null) {
         if (current.needsInvalidate) {
            HttpSession session = current.getSession();
            if (session != null)
               session.invalidate();
         }
         currentContextStore.set(null);
      }
      ScopeContext requestCtx = RequestScopeDefinition.getRequestScopeDefinition().getScopeContext(false);
      if (requestCtx != null) {
         requestCtx.scopeDestroyed(null);
      }
   }

   void markSessionInvalid() {
      needsInvalidate = true;
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
         ctx.log("session invalidated");

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

            int sessionPart = getSessionWindowIdPart();
            windowId = sessionPart + nextWindowId;
            String queryStr = request.getQueryString();
            String fullURL = queryParams == null ? requestURL : requestURL + "?" + queryStr;
            String userAgent = request.getHeader("User-Agent");
            windowCtx = new WindowScopeContext(windowId, Window.createNewWindow(fullURL, request.getServerName(), request.getServerPort(), request.getRequestURI(), request.getPathInfo(), queryStr, userAgent));
            windowCtx.init();
            windowCtx.lastRequestTime = System.currentTimeMillis();
            ctxList.add(windowCtx);

            SyncManager.addSyncInst(windowCtx.window, true, false, true, "window", null);

            if (verbose) {
               log("new window");
            }

            // First we look for the first non-waiting window to remove. Then we just stop the waiter and remove it anyway.
            if (!enforceMaxWindows(ctxList, false))
               enforceMaxWindows(ctxList, true);
         }
         PTypeUtil.setWindowId(windowId);
      }
      return windowCtx;
   }

   private final static int WindowIdSeparatorScale = 100;

   // Include some integer based on the session id in the windowId to make it resilient to restarts.
   // Although windowId only has to be unique within a given session and is not used for authentication access - that's
   // done at the session level, if a server restarts, an old tab with windowId=0 would get confused with a new one
   // A side benefit is that we can tell when we're getting a request from a session we don't recognize and flag
   // that in the logs
   private int getSessionWindowIdPart() {
      if (testMode) // TODO: This makes the tests more consistent, we might run into problems if we start testing restart
         return 0;
      String sessionId = session.getId();
      int sessionPart = 0;
      for (int i = 0; i < sessionId.length(); i++)
         sessionPart += sessionId.charAt(i);
      sessionPart = sessionPart % 1000000 * WindowIdSeparatorScale;
      return sessionPart;
   }

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
            log("removing window scope: " + windowId + " for: " + toRem.getTraceId() + " - exceeded max windows per session: " + numCtxs);
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
         sb.append("app:");
         sb.append(requestURI);
      }
      if (session != null) {
         sb.append(" session:");
         sb.append(DynUtil.getTraceId(session.getId()));
      }
      // For normal windows, log just the simple integer id 0, 1, etc but when we see that the sessionIdpart
      // does not match, it means this window was created from a different session and so it's been a restart or
      // failover so highlight that here in the context
      if (windowCtx != null) {
         int winId = windowCtx.windowId;
         int winSessionIdPart = (winId / WindowIdSeparatorScale) * WindowIdSeparatorScale;
         if (winSessionIdPart == getSessionWindowIdPart()) {
            sb.append(" window:");
            sb.append(windowCtx.windowId % WindowIdSeparatorScale);
         }
         else {
            sb.append(" window:");
            sb.append(windowCtx.windowId);
            sb.append("(recovered)"); // created in a different session so must have been restored
         }
      }
      sb.append(" thread:" + threadName);
      return sb.toString();
   }

   static long lastLogTime = -1;

   static String getTimeString() {
      long now = System.currentTimeMillis();
      if (lastLogTime == -1)
         lastLogTime = now;
      String res = PerfMon.formatTime(now) + PerfMon.getTimeDelta(lastLogTime, now);
      lastLogTime = now;
      return res;
   }

   public void log(String message) {
      String line = getTimeString() + " " + toString() + " - " + message;
      outputLine(line);
   }

   public void error(String message) {
      String line = "Error: " + getTimeString() + " " + toString() + " - " + message;
      outputLine(line);
   }

   private void outputLine(String line) {
      if (request != null)
         request.getServletContext().log(line);
      else if (session != null)
         session.getServletContext().log(line);
      else
         System.err.println("Context missing request/session with error: " + line);
   }

   public static void logForRequest(HttpServletRequest request, String message) {
      String line = getTimeString() + "app:" + request.getRequestURI() + " - " + message;
      request.getServletContext().log(line);
      //System.out.println(line);
   }

   /** Returns useful info in string form to identify the details of the particular request */
   public String getRequestDetail() {
      StringBuilder sb = new StringBuilder();
      sb.append(request.getMethod());
      sb.append(" ");
      List<String> hdrNames = new ArrayList<String>();
      hdrNames.add("User-Agent");
      hdrNames.add("Referer");
      hdrNames.add("X-Forwarded-For");
      boolean first = true;
      for (int i = 0; i < hdrNames.size(); i++) {
         if (!first) {
            sb.append("\n");
         }
         else
            first = false;
         String hdrName = hdrNames.get(i);
         String val = request.getHeader(hdrName);
         if (val != null)
            sb.append(hdrName + ": " + val);
         else if (val == null) {
            log("header: " + hdrName + " is null");
         }
      }
      sb.append("\n");
      return sb.toString();
   }

   private void requestCompleteError() {
      if (requestComplete) {
         if (redirectURL != null)
            throw new IllegalArgumentException("Response already redirected to: " + redirectURL);
         else if (requestError != null)
            throw new IllegalArgumentException("Attempt to redirect after error response: " + requestError);
         else
            throw new IllegalArgumentException("Attempt to redirect on completed request");
      }
   }

   public void sendRedirect(String toURL) {
      try {
         requestCompleteError();
         if (verbose)
            log("Redirecting to: " + toURL);
         redirectURL = toURL;
         requestComplete = true;
         response.sendRedirect(toURL);
      }
      catch (IOException exc) {
         if (verbose)
            log("IOException in sendRedirect(" + toURL + ")" + exc);
      }
   }

   public void sendError(int errorCode, String msg) {
      try {
         requestCompleteError();
         if (verbose)
            log("Response error code: " + errorCode + " message:" + msg);
         requestError = msg;
         requestComplete = true;
         response.sendError(errorCode, msg);
      }
      catch (IOException exc) {
         if (verbose)
            log("IOException in sendError(" + errorCode + ", " + msg);
      }
   }

   public Cookie getCookie(String name) {
      if (cookieMap == null) {
         cookieMap = new TreeMap<String,Cookie>();
         Cookie[] cookies = request.getCookies();
         if (cookies != null) {
            for (Cookie cookie: cookies)
               cookieMap.put(cookie.getName(), cookie);
         }
      }
      return cookieMap.get(name);
   }

   public void startFileUpload(UploadServerConfig config) {
      if (config == null)
         config = defaultUploadConfig;
      platformEnableFileUpload(config);
   }

   public void platformEnableFileUpload(UploadServerConfig config) {
      throw new UnsupportedOperationException();
   }

   public static String buildRequestURL(HttpServletRequest request, String uri) {
      StringBuilder builder = new StringBuilder();
      builder.append(request.getProtocol());
      builder.append("://");
      builder.append(request.getServerName());
      if (request.getServerPort() != 80 && request.getServerPort() != 443) {
         builder.append(":");
         builder.append(request.getServerPort());
      }
      builder.append("/");
      builder.append(uri);
      return builder.toString();
   }

}

import java.io.PrintWriter;
import sc.sync.SyncManager;

import sc.dyn.DynUtil;
import sc.obj.ScopeDefinition;
import sc.obj.ScopeContext;

import sc.type.PTypeUtil;
import sc.lang.html.Window;

import java.util.Enumeration;

class Context {
   HttpServletRequest request;
   HttpServletResponse response;
   HttpSession session;
   String mimeType;
   boolean mimeTypeSet = false;
   boolean requestComplete;
   boolean windowRequest = true; // When processing a session invalidate event, we are not from a window

   ArrayList<ServletScheduler.ScheduledJob> toInvokeLater = null;

   Context(HttpServletRequest req, HttpServletResponse res) {
      request = req;
      response = res;
   }

   /** Use this in session destruction hook, when request/response is not available */
   Context(HttpSession session) {
      this.session = session;
   }


   HttpSession getSession() {
      if (request == null)
         return session;
      return request.getSession(true);
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
            for (WindowScopeContext winCtx:ctxList) {
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
      windowCtx = getWindowScopeContext();
      windowCtx.windowId = windowId;
      updateWindowContext(windowCtx);
      return windowCtx;
   }

   void updateWindowContext(WindowScopeContext winCtx) {
      windowCtx = winCtx;
      if (winCtx != null)
         PTypeUtil.setWindowId(winCtx.windowId);
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

   static Context initContext(HttpServletRequest request, HttpServletResponse response) {
      Context ctx;
      currentContextStore.set(ctx = new Context(request, response));

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
         ctx.updateWindowContext(ctx.getWindowScopeContext());
      }
      // TODO: populate this with data from the request/response - url and compute a size from the device meta-data
      // You might want to render different content based on the device size for example so that would be nice to have here.
      // Of course the location for rendering links the same between client and server
      Window.setWindow(ctx.windowCtx.getWindow());
      return ctx;
   }

   static void clearContext() {
      currentContextStore.set(null);
   }

   void execLaterJobs() {
      if (toInvokeLater != null) {
         SyncManager.SyncState origState = null;
         try {
            origState = SyncManager.getSyncState();
            // While running any callbacks, we are in the recording state, even if invoking these as part of the
            // initialization phase.  This is really like Initializing but where there's a nested binding count.
            SyncManager.setSyncState(SyncManager.SyncState.RecordingChanges);
            ArrayList<ServletScheduler.ScheduledJob> toRun = (ArrayList<ServletScheduler.ScheduledJob>)toInvokeLater.clone();
            for (ServletScheduler.ScheduledJob sj:toRun) {
               sj.toInvoke.run();
            }
         }
         finally {
            SyncManager.setSyncState(origState);
            toInvokeLater = null;
         }
      }
   }

   WindowScopeContext windowCtx = null;

   public void setWindowId(int windowId) {
      windowCtx.windowId = windowId;
   }

   public int getWindowId() {
      return getWindowScopeContext().windowId;
   }

   public static boolean getHasWindowScope() {
      return getWindowScope() != null;
   }

   public static WindowScopeContext getWindowScope() {
      Context current = getCurrentContext();
      if (current == null)
         return null;
      return current.getWindowScopeContext();
   }

   public void destroyWindowScopes() {
      HttpSession session = getSession();
      if (session == null)
         return;
      ArrayList<WindowScopeContext> ctxList = (ArrayList<WindowScopeContext>) session.getAttribute("_windowContexts");
      if (ctxList != null) {
         for (WindowScopeContext winScope:ctxList) {
            winScope.scopeDestroyed();
         }
         ctxList.clear();
      }
   }

   public static void destroyContext(HttpSession session) {
      Context ctx = Context.initContext(session);

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
            scopeCtx.scopeDestroyed();
      }
      finally {
         Context.clearContext();
      }
   }

   public WindowScopeContext getWindowScopeContext() {
      if (windowCtx == null && windowRequest) {
         HttpSession session = getSession();
         if (session == null)
            return null;
         ArrayList<WindowScopeContext> ctxList = (ArrayList<WindowScopeContext>) session.getAttribute("_windowContexts");
         int windowId;
         if (ctxList == null) {
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
            windowId = ctxList.size();
            windowCtx = new WindowScopeContext(windowId, Window.createNewWindow(request.getRequestURL().toString(), request.getServerName(), request.getServerPort(), request.getRequestURI(), request.getPathInfo()));
            ctxList.add(windowCtx);
         }
         PTypeUtil.setWindowId(windowId);
      }
      return windowCtx;
   }

   void write(String str) {
      PrintWriter writer;
      if (mimeType != null && !mimeTypeSet) {
         response.setContentType(mimeType);
         mimeTypeSet = true;
      }
      try {
         writer = response.getWriter();
         writer.print(str);
         writer.flush();
      }
      catch (IOException exc) {
         throw new IllegalArgumentException("failed to write to client: " + exc.toString());
      }
   }
}

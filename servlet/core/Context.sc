import java.io.PrintWriter;
import sc.sync.SyncManager;

import sc.dyn.DynUtil;
import sc.dyn.ScheduledJob;
import sc.obj.ScopeDefinition;
import sc.obj.ScopeContext;
import sc.obj.RequestScopeDefinition;
import sc.obj.CurrentScopeContext;

import sc.type.PTypeUtil;
import sc.lang.html.Window;

import java.util.Enumeration;

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

   ArrayList<ScheduledJob> toInvokeLater = null;

   WindowScopeContext windowCtx = null;
  
   /** Set to true when the server is in the midst of a shutdown */
   public static boolean shuttingDown = false;
   /** Set this to true when the server will restart */
   public static boolean restarting = false;

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
      windowCtx = getWindowScopeContext(true);
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
         requestCtx.scopeDestroyed();
      }
   }

   void execLaterJobs() {
      if (toInvokeLater != null) {
         SyncManager.SyncState origState = null;
         try {
            origState = SyncManager.getSyncState();
            // While running any callbacks, we are in the recording state, even if invoking these as part of the
            // initialization phase.  This is really like Initializing but where there's a nested binding count.
            SyncManager.setSyncState(SyncManager.SyncState.RecordingChanges);
            ArrayList<ScheduledJob> toRun = (ArrayList<ScheduledJob>)toInvokeLater.clone();
            for (ScheduledJob sj:toRun) {
               sj.toInvoke.run();
            }
         }
         finally {
            SyncManager.setSyncState(origState);
            toInvokeLater = null;
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
            winScope.scopeDestroyed();
            String scopeAlias = (String) winScope.getValue("scopeAlias");
            if (scopeAlias != null) {
               if (!CurrentScopeContext.remove(scopeAlias))
                  System.err.println("*** Failed to remove CurrentScopeContext for alias: " + scopeAlias);
            }
         }
         ctxList.clear();
         session.removeAttribute("_windowContexts");
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
            windowId = ctxList.size();
            windowCtx = new WindowScopeContext(windowId, Window.createNewWindow(request.getRequestURL().toString(), request.getServerName(), request.getServerPort(), request.getRequestURI(), request.getPathInfo()));
            windowCtx.init();
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

   boolean hasDoLaterJobs() {
      return toInvokeLater != null && toInvokeLater.size() > 0;
   }
}

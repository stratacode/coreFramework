import sc.obj.ScopeContext;
import sc.obj.ScopeDefinition;
import sc.obj.GlobalScopeDefinition;
import sc.obj.AppGlobalScopeDefinition;

// Using startPriority here to assign the scopeIds in scope precedence order (e.g. global, appGlobal, session, appSession, window, request)
@CompilerSettings(createOnStartup=true, startPriority=105)
object AppSessionScopeDefinition extends ScopeDefinition {
   name = "appSession";
   eventListenerCtx = true;
   //supportsChangeEvents = true; The window scope should become a child instance for app-session - we deliver the events there... that's how we keep track of whether each window has received the changes
   {
      addParentScope(AppGlobalScopeDefinition);
      addParentScope(SessionScopeDefinition);
   }

   private AppSessionScopeDefinition() {
      super(3);
   }

   public static ScopeContext getAppSessionScope() {
      return AppSessionScopeDefinition.getScopeContext(true);
   }

   public ScopeContext getScopeContext(boolean create) {
      HttpSession session = Context.getCurrentSession();
      if (session == null)
         return super.getScopeContext(create);
      AppSessionScopeContext ctx;
      String appId = sc.type.PTypeUtil.getAppId();
      String key = "_sessionApp_" + appId;
      try {
         ctx = (AppSessionScopeContext) session.getAttribute(key);
         if (ctx == null && create) {
            synchronized (session) {
               ctx = (AppSessionScopeContext) session.getAttribute(key);
               if (ctx == null) {
                  ctx = new AppSessionScopeContext(session, appId);
                  if (ScopeDefinition.verbose)
                      System.out.println("Creating appSession context for app: " + appId + " session: " + session.getId());
                  session.setAttribute(key, ctx);
                  ctx.init();
               }
            }
         }
      }
      catch (IllegalStateException exc) {
         if (ScopeDefinition.verbose)
            System.out.println("Session expired for app: " + appId);
         return null;
      }
      return ctx;
   }

   public ScopeDefinition getScopeDefinition() {
      return AppSessionScopeDefinition;
   }

   public String getDescription() {
      return "Stores info specific to a browser session for a particular application (usually the 'base url of the page'). Info persists using the app server's session. ";
   }
}

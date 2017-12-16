import sc.obj.ScopeContext;
import sc.obj.ScopeDefinition;
import sc.obj.ScopeEnvironment;
import sc.obj.GlobalScopeDefinition;
import sc.obj.AppGlobalScopeDefinition;

// Using startPriority here to assign the scopeIds in scope precedance order (e.g. global, appGlobal, session, appSession, window, request)
@CompilerSettings(createOnStartup=true, startPriority=105)
object AppSessionScopeDefinition extends ScopeDefinition {
   name = "appSession";
   eventListenerCtx = true;
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
      String appId = ScopeEnvironment.getAppId();
      String key = "_sessionApp_" + appId;
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
      return ctx;
   }

   public ScopeDefinition getScopeDefinition() {
      return AppSessionScopeDefinition;
   }
}

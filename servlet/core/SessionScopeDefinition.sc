import sc.obj.ScopeDefinition;
import sc.obj.ScopeContext;
import sc.obj.GlobalScopeDefinition;
import sc.obj.AppGlobalScopeDefinition;

@CompilerSettings(createOnStartup=true,startPriority=100)
object SessionScopeDefinition extends ScopeDefinition {
   name = "session";
   {
      addParentScope(GlobalScopeDefinition.getGlobalScopeDefinition());
      addParentScope(AppGlobalScopeDefinition.getAppGlobalScopeDefinition());
   }

   public ScopeContext getScopeContext(boolean create) {
      HttpSession session = Context.getCurrentSession();
      if (session == null)
         return null;
      SessionScopeContext ctx;
      ctx = (SessionScopeContext) session.getAttribute("_sessionScopeContext");
      if (ctx == null && create) {
         synchronized (session) {
            ctx = (SessionScopeContext) session.getAttribute("_sessionScopeContext");
            if (ctx == null) {
               ctx = new SessionScopeContext(session);
               if (ScopeDefinition.verbose)
                   System.out.println("Creating session context: " + session.getId());
               session.setAttribute("_sessionScopeContext", ctx);
            }
         }
      }
      return ctx;
   }

   public ScopeDefinition getScopeDefinition() {
      return SessionScopeDefinition;
   }
}

import sc.obj.ScopeDefinition;
import sc.obj.ScopeContext;
import sc.obj.GlobalScopeDefinition;

@CompilerSettings(createOnStartup=true,startPriority=100)
object SessionScopeDefinition extends ScopeDefinition {
   name = "session";
   parentScope = GlobalScopeDefinition.getGlobalScopeDefinition();

   public ScopeContext getScopeContext() {
      HttpSession session = Context.getCurrentSession();
      if (session == null)
         return null;
      SessionScopeContext ctx;
      ctx = (SessionScopeContext) session.getAttribute("_sessionScopeContext");
      if (ctx == null) {
         synchronized (session) {
            ctx = (SessionScopeContext) session.getAttribute("_sessionScopeContext");
            if (ctx == null) {
               ctx = new SessionScopeContext(session);
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

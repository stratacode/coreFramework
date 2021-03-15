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
      // If no more specific listener is registered we'll collect
      // binding events here.  We might not have a global definition
      eventListenerCtx = true;
   }

   SessionScopeDefinition() {
      super(2);
   }

   public static ScopeContext getSessionScope() {
      return AppSessionScopeDefinition.getScopeContext(true);
   }

   public ScopeContext getScopeContext(boolean create) {
      HttpSession session = Context.getCurrentSession();
      if (session == null)
         return super.getScopeContext(create);
      SessionScopeContext ctx;

      try {
         ctx = (SessionScopeContext) session.getAttribute("_sessionScopeContext");
         if (ctx == null && create) {
            synchronized (session) {
               ctx = (SessionScopeContext) session.getAttribute("_sessionScopeContext");
               if (ctx == null) {
                  ctx = new SessionScopeContext(session);
                  if (ScopeDefinition.verbose)
                      System.out.println("Creating session context: " + session.getId());
                  session.setAttribute("_sessionScopeContext", ctx);
                  ctx.init();
               }
            }
         }
      }
      catch (IllegalStateException exc) {
         System.err.println("*** Attempt to access session scope with expired session" + exc + " create: " + create);
         if (create)
            throw exc; // Unable to create it in this situation
         return null; // It's already been destroyed so just return null
      }
      return ctx;
   }

   public ScopeDefinition getScopeDefinition() {
      return SessionScopeDefinition;
   }

   public String getDescription() {
      return "Stores info specific to a browser session";
   }
}

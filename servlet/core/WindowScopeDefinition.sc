import sc.obj.ScopeDefinition;
import sc.obj.ScopeContext;
import sc.obj.GlobalScopeDefinition;

@CompilerSettings(createOnStartup=true,startPriority=95)
object WindowScopeDefinition extends ScopeDefinition {
   name = "window";

   {
      addParentScope(AppSessionScopeDefinition);
   }

   private WindowScopeDefinition() {
      super(4);
   }

   public ScopeContext getScopeContext(boolean create) {
      Context ctx = Context.getCurrentContext();
      return ctx == null ? null : ctx.getWindowScopeContext(create);
   }

   public ScopeDefinition getScopeDefinition() {
      return WindowScopeDefinition;
   }
}

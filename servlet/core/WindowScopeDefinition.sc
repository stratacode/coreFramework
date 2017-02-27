import sc.obj.ScopeDefinition;
import sc.obj.ScopeContext;
import sc.obj.GlobalScopeDefinition;

@CompilerSettings(createOnStartup=true,startPriority=95)
object WindowScopeDefinition extends ScopeDefinition {
   name = "window";

   {
      addParentScope(AppSessionScopeDefinition);
   }

   public ScopeContext getScopeContext(boolean create) {
      return Context.getCurrentContext().getWindowScopeContext(create);
   }

   public ScopeDefinition getScopeDefinition() {
      return WindowScopeDefinition;
   }
}

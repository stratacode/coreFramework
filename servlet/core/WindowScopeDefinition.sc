import sc.obj.ScopeDefinition;
import sc.obj.ScopeContext;
import sc.obj.GlobalScopeDefinition;

@CompilerSettings(createOnStartup=true,startPriority=100)
object WindowScopeDefinition extends ScopeDefinition {
   name = "window";

   parentScope = SessionScopeDefinition;

   public ScopeContext getScopeContext() {
      return Context.getCurrentContext().getWindowScopeContext();
   }

   public ScopeDefinition getScopeDefinition() {
      return WindowScopeDefinition;
   }
}

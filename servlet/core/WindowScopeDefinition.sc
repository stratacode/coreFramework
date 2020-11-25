import sc.obj.ScopeDefinition;
import sc.obj.ScopeContext;
import sc.obj.GlobalScopeDefinition;

@CompilerSettings(createOnStartup=true,startPriority=95)
object WindowScopeDefinition extends ScopeDefinition {
   name = "window";
   eventListenerCtx = true;
   supportsChangeEvents = true;

   /**
     * Each time the user goes to a new page in  your site, they create a new windowContext.
     * It's good that we maintain the page history, and we need to keep alive windows where
     * there's still an outstanding sync - maybe the user created a new tab. But it's nice to
     * limit the number of these for each browser session because if a user is clicking on a
     * number of pages, at some point, we want to reclaim that memory if they will never go back to it.
     */
   int maxWindowsPerSession = 5;

   {
      addParentScope(AppSessionScopeDefinition);
   }

   private WindowScopeDefinition() {
      super(4);
   }

   public static ScopeContext getWindowScope() {
      return WindowScopeDefinition.getScopeContext(true);
   }

   public ScopeContext getScopeContext(boolean create) {
      Context ctx = Context.getCurrentContext();
      return ctx == null ? super.getScopeContext(create) : ctx.getWindowScopeContext(create);
   }

   public ScopeDefinition getScopeDefinition() {
      return WindowScopeDefinition;
   }

   public String getDescription() {
      return "Stores info for all requests made by the same browser window/tab. As a user navigates within the site, the windows' they visit store the history info and make the back button easier and more efficient. There's a limit of maxWindowsPerSession though for how many are retained. A window scope context may have an outstanding sync request from the browser.";
   }
}

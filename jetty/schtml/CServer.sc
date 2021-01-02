import sc.servlet.Context;
import sc.obj.GlobalScopeDefinition;
import sc.obj.ScopeContext;

CServer {
   void startShutdown() {
      Context.shuttingDown = true;
      System.out.println("Server - starting shutdown");
      ScopeContext ctx = GlobalScopeDefinition.getScopeContext(false);
      // Need to wake up any sync servlets that are waiting 
      if (ctx != null) {
         ctx.scopeChanged();
         DynUtil.execLaterJobs();

         System.out.println("Server - closing global scope");

         try {
            ctx.scopeDestroyed(null);
         }
         catch (RuntimeException exc) {
            exc.printStackTrace();
            System.err.println("*** Error closing global scope: " + exc);
         }

         System.out.println("Server waiting to shutdown");
         // This pause here gives any outstanding sync requests a chance to wake up and return a special status
         // to their clients.  For testing in particular, it means we get a clean shutdown and stop polling.
         try {
            Thread.sleep(2000);
         }
         catch (InterruptedException exc) {}
      }
      System.out.println("Server shutting down");
   }
}

import sc.servlet.Context;
import sc.obj.GlobalScopeDefinition;
import sc.obj.ScopeContext;

CServer {
   // Replacing the previous handlers by redefining the HandlerList
   object handlerList extends HandlerList {
      object webAppHandler extends WebAppContext {
         // Check for the parent class loaders before loading them.
         // Otherwise, we can end up with the same class getting loaded twice
         // and conflicts result.  Alternatively, we could try to inject the web-app class loader
         // into the system so we consistently use that?   This seems like saner behavior even if
         // it is not 2.3 servlet spec compliant.
         parentLoaderPriority = true;

         contextPath = "/";
         war="./web";
      }
   }

   void startShutdown() {
      Context.shuttingDown = true;
      ScopeContext ctx = GlobalScopeDefinition.getScopeContext(false);
      // Need to wake up any sync servlets that are waiting 
      if (ctx != null) {
         ctx.scopeChanged();
         DynUtil.execLaterJobs();
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

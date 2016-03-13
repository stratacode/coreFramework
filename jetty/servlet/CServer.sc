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
}

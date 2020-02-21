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
         // We're using relative path names here so that we can reduce the dependencies on path names in 
         // the generated runtime - so you can deploy and test the same app anywhere.
         // But between Java 8 and 9, it's not possible to change the actual current process directory.
         // To support scc's compile+run from one process, it changes this system property before launching main
         war = sc.util.FileUtil.concat(System.getProperty("user.dir"), "/web");
      }
   }

}

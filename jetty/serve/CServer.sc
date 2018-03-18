object CServer extends Server {
   object httpConnector extends ServerConnector {
      // TODO: should this value be derived from system.URL at some point?
      port = 8080;
   }
   
   object handlerList extends HandlerList {
      object resourceHandler extends ResourceHandler {
         welcomeFiles = {"index.html"};
         // Run the server from the web subdirectory. 
         resourceBase = "./web";
      }
      object defaultHandler extends DefaultHandler {
      }
   }

   boolean sync = false;

   boolean stopped = false;

   static CServer theServer;
   
   @sc.obj.MainSettings(produceScript = true, execName = "startSCJetty", debug = false, stopMethod="stopServer")
   static void main(String[] args) throws Exception {
      CServer s = theServer = CServer;
      if (s.sync)
         s.join();
   }

   void startShutdown() {
   }

   static void stopServer() {
      try {
         if (theServer != null) {
            theServer.startShutdown();
            theServer.stop();
         }
         // else - we may not have called 'main' in if run 'scc -c ...'
      }
      catch (Exception exc) {
         System.err.println("*** Failed to stop jetty server: " + exc);
         exc.printStackTrace();
      }
   }
}

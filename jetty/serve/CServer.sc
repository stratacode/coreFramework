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
   
   @sc.obj.MainSettings(produceScript = true, execName = "startSCJetty", debug = false)
   static void main(String[] args) throws Exception {
      CServer s = CServer;
      if (s.sync)
         s.join();
   }
}

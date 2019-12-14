import org.eclipse.jetty.webapp.Configuration;

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

   // Adds support for java:comp/env jndi name space and some other J2EE stuff
   // Using _init because this class uses IAltComponent to avoid conflicts with Jetty's init method
   void _init() {
      Configuration.ClassList classlist = Configuration.ClassList
            .setServerDefault(server);
      classlist.addAfter(
          "org.eclipse.jetty.webapp.FragmentConfiguration",
          "org.eclipse.jetty.plus.webapp.EnvConfiguration",
          "org.eclipse.jetty.plus.webapp.PlusConfiguration");
      /*
      classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
          "org.eclipse.jetty.annotations.AnnotationConfiguration");
       */
   }
   
   //@sc.obj.MainSettings(produceScript = true, execName = "startSCJetty", debug = false, stopMethod="stopServer")
   @sc.obj.MainSettings(produceScript = true, execName = "startSCJetty", debug = false,
                        stopMethod="stopServer", produceJar=true, includeDepsInJar=true)
   static void main(String[] args) throws Exception {
      System.setProperty("java.naming.factory.url.pkgs", "org.eclipse.jetty.jndi");
      System.setProperty("java.naming.factory.initial", "org.eclipse.jetty.jndi.InitialContextFactory");

      if (args.length > 0) {
         for (String arg:args) {
            if (arg.equals("-vh")) {
               sc.lang.html.Element.verbose = true;
            }
            else if (arg.equals("-vs"))
               sc.sync.SyncManager.verbose = true;
         }
      }

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

import org.eclipse.jetty.server.nio.SelectChannelConnector;

public class ServerConnector extends SelectChannelConnector {
   public ServerConnector(Server server) {
       super();
       setServer(server);
   }
}

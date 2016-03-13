import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpSessionEvent;

@CompilerSettings(mixinTemplate="sc.type.InitTypesMixin")
public class PageInit extends BasePageInit implements ServletContextListener, HttpSessionListener {
    public void contextInitialized(ServletContextEvent event) {
       initTypes();
    }

    public void contextDestroyed(ServletContextEvent event) {
    }

    public void sessionCreated(HttpSessionEvent event) {
    }

    public void sessionDestroyed(HttpSessionEvent event) {
       Context.destroyContext(event.getSession());
    }
}

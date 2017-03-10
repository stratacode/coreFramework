import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpSessionEvent;

import sc.obj.RequestScopeDefinition;
import sc.servlet.WindowScopeDefinition;

@CompilerSettings(mixinTemplate="sc.type.InitTypesMixin")
public class PageInit extends BasePageInit implements ServletContextListener, HttpSessionListener {
    public void contextInitialized(ServletContextEvent event) {
       sc.obj.ScopeDefinition.initScopes();
       initTypes();
       // For servlets, the request is chained off of the window as well as 'global'
       RequestScopeDefinition.addParentScope(WindowScopeDefinition);
    }

    public void contextDestroyed(ServletContextEvent event) {
    }

    public void sessionCreated(HttpSessionEvent event) {
    }

    public void sessionDestroyed(HttpSessionEvent event) {
       Context.destroyContext(event.getSession());
    }
}

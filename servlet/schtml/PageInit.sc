import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpSessionEvent;

import sc.obj.RequestScopeDefinition;
import sc.servlet.WindowScopeDefinition;
import sc.layer.LayeredSystem;

@CompilerSettings(mixinTemplate="sc.type.InitTypesMixin")
public class PageInit extends BasePageInit implements ServletContextListener, HttpSessionListener {
    public void contextInitialized(ServletContextEvent event) {
       ServletScheduler.init();
       sc.obj.ScopeDefinition.initScopes();
       initTypes();
       // For servlets, the request is chained off of the window as well as 'global'
       RequestScopeDefinition.addParentScope(WindowScopeDefinition);
       LayeredSystem sys = LayeredSystem.getCurrent();
       // Call back to the layered system to let it know the sync types etc are initialized.  We might need to init the sync for the command interpreter

       PageDispatcher.initPageEntries();

       if (sys != null)
          sys.runtimeInitialized();
    }

    public void contextDestroyed(ServletContextEvent event) {
    }

    public void sessionCreated(HttpSessionEvent event) {
    }

    public void sessionDestroyed(HttpSessionEvent event) {
       Context.destroyContext(event.getSession());
    }
}

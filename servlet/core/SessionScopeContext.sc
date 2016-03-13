import sc.dyn.DynUtil;
import sc.obj.ScopeDefinition;
import sc.obj.ScopeContext;

import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionBindingEvent;

class SessionScopeContext extends ScopeContext implements HttpSessionBindingListener {
   HttpSession session;
   SessionScopeContext(HttpSession session) {
      this.session = session;
   }
   Object getValue(String key) {
      return session.getAttribute(key);
   }

   public void setValue(String key, Object value) {
      session.setAttribute(key, value);
   }

   public ScopeDefinition getScopeDefinition() {
      return SessionScopeDefinition;
   }

   // Called when we are added to the session
   public void valueBound(HttpSessionBindingEvent httpSessionBindingEvent) {
   }

   // Called when the session is expired or we are removed
   public void valueUnbound(HttpSessionBindingEvent httpSessionBindingEvent) {
   }

   public String getId() {
      return session.getId();
   }

   public boolean isCurrent() {
      return Context.getCurrentSession() == session;
   }
}

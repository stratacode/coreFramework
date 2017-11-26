import sc.dyn.DynUtil;
import sc.obj.ScopeDefinition;
import sc.obj.ScopeContext;

import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionBindingEvent;

import java.util.Map;
import java.util.LinkedHashMap;

class SessionScopeContext extends ScopeContext implements HttpSessionBindingListener {
   HttpSession session;
   LinkedHashMap<String,Object> valuesMap;

   SessionScopeContext(HttpSession session) {
      this.session = session;
   }
   Object getValue(String key) {
      return session.getAttribute(key);
   }

   public void setValue(String key, Object value) {
      if (ScopeDefinition.trace)
         System.out.println("Scope: session - setValue " + key + " = " + value + " for: " + toString());
      session.setAttribute(key, value);
      if (valuesMap == null) {
         valuesMap = new LinkedHashMap<String,Object>();
      }
      valuesMap.put(key, value);
   }

   public Map<String,Object> getValues() {
      return valuesMap;
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
      return "session:" + DynUtil.getTraceObjId(session.getId());
   }

   public boolean isCurrent() {
      return Context.getCurrentSession() == session;
   }

   public String toString() {
      return getId();
   }
}

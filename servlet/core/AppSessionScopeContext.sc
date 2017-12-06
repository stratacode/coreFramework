import sc.dyn.DynUtil;
import sc.obj.ScopeContext;
import sc.obj.ScopeDefinition;
import sc.obj.ScopeEnvironment;

import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionBindingEvent;

import java.util.Map;
import java.util.LinkedHashMap;

class AppSessionScopeContext extends ScopeContext implements HttpSessionBindingListener {
   HttpSession session;
   String appId;
   LinkedHashMap<String,Object> valuesMap;

   AppSessionScopeContext(HttpSession session, String appId) {
      this.session = session;
      this.appId = appId;
   }

   // Just like with SessionScopeContext using the session to store values directly because
   // app servers provide management features like session persistence which we can use more
   // efficiently if we don't wrap everything in one hash-map where we would have to manage
   // the save/restore and incremental updates could not be optimized by the app server
   Object getValue(String key) {
      return session.getAttribute(appId + "__" + key);
   }

   public void setValue(String key, Object value) {
      if (ScopeDefinition.trace)
         System.out.println("Scope: appSession - set value " + key + " = " + value + " for: " + toString());
      // Stored in the session so we can use the session persistence of the app server
      session.setAttribute(appId + "__" + key, value);
      if (valuesMap == null) {
         valuesMap = new LinkedHashMap<String,Object>();
      }
      valuesMap.put(key, value);
   }

   public Map<String,Object> getValues() {
      return valuesMap;
   }

   public ScopeDefinition getScopeDefinition() {
      return AppSessionScopeDefinition;
   }

   // Called when we are added to the session
   public void valueBound(HttpSessionBindingEvent httpSessionBindingEvent) {
   }

   // Called when the session is expired or we are removed
   public void valueUnbound(HttpSessionBindingEvent httpSessionBindingEvent) {
      // TODO: iterate over keys we've added and remove them?
   }

   public String getId() {
      return "appSession:" + appId + "__" + DynUtil.getTraceObjId(session.getId());
   }

   public boolean isCurrent() {
      return Context.getCurrentSession() == session && sc.util.StringUtil.equalStrings(ScopeEnvironment.getAppId(), appId);
   }

   public String toString() {
      return getId();
   }
}

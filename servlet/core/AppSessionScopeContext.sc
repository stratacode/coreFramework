import sc.dyn.DynUtil;
import sc.obj.ScopeContext;
import sc.obj.ScopeDefinition;

import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionBindingEvent;

import java.util.Map;
import java.util.LinkedHashMap;

class AppSessionScopeContext extends ScopeContext implements HttpSessionBindingListener {
   HttpSession session;
   String appId;
   protected LinkedHashMap<String,Object> valuesMap;
   protected LinkedHashMap<String,Object> refMap;

   AppSessionScopeContext(HttpSession session, String appId) {
      this.session = session;
      this.appId = appId;
   }

   // Just like with SessionScopeContext using the session to store values directly because
   // app servers provide management features like session persistence which we can use more
   // efficiently if we don't wrap everything in one hash-map where we would have to manage
   // the save/restore and incremental updates could not be optimized by the app server
   Object getValue(String key) {
      try {
         return session.getAttribute(appId + "__" + key);
      }
      catch (IllegalStateException exc) {
         // When the session is expired on this request we may access the session afterwards - like in setInitialSync
         return null;
      }
   }

   public void setValue(String key, Object value) {
      // Stored in the session so we can use the session persistence of the app server
      try {
         session.setAttribute(appId + "__" + key, value);
      }
      catch (IllegalStateException exc) {
         if (ScopeDefinition.verbose)
            System.out.println("Scope: appSession - set value " + key + " = " + value + " session not updated because it has been invalidated");
      }
      if (ScopeDefinition.trace)
         System.out.println("Scope: appSession - set value " + key + " = " + value + " for: " + toString());
      if (valuesMap == null) {
         valuesMap = new LinkedHashMap<String,Object>();
      }
      valuesMap.put(key, value);
   }

   public void setValueByRef(String name, Object value) {
      if (refMap == null)
         refMap = new LinkedHashMap<String,Object>();
      refMap.put(name, value);
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
      return Context.getCurrentSession() == session && sc.util.StringUtil.equalStrings(sc.type.PTypeUtil.getAppId(), appId);
   }

   public String toString() {
      return getId();
   }
}

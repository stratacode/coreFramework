import sc.obj.ScopeDefinition;
import sc.obj.ScopeContext;

import sc.lang.html.Window;

import sc.dyn.DynUtil;

import java.util.HashMap;

public class WindowScopeContext extends ScopeContext {
   // The integer identifying this window in this session - from 0 as the first page in the session
   public int windowId;

   // The javascript window object
   Window window;

   HashMap<String,Object> values;

   public WindowScopeContext(int windowId, Window window) {
      this.windowId = windowId;
      this.window = window;
   }
   Object getValue(String key) {
      if (values == null)
         return null;
      return values.get(key);
   }

   public void setValue(String key, Object value) {
      if (values == null)
         values = new HashMap<String,Object>();
      values.put(key, value);
   }

   public ScopeDefinition getScopeDefinition() {
      return WindowScopeDefinition;
   }

   public String getId() {
      // TODO: should this include the session id?
      return String.valueOf(windowId);
   }

   public Window getWindow() {
      return window;
   }

   public boolean isCurrent() {
      Context ctx = Context.getCurrentContext();
      return ctx != null && ctx.windowCtx == this;
   }

   public void scopeDestroyed() {
      if (values != null) {
         ArrayList<String> keysToDestroy = new ArrayList<String>(values.size());
         for (String key:values.keySet()) {
            keysToDestroy.add(key);
         }
         for (int i = 0; i < keysToDestroy.size(); i++) {
            Object value = values.get(keysToDestroy.get(i));
            if (value != null) {
               DynUtil.dispose(value);
            }
         }
         values = null;
      }
      // Destroy the sync context after we dispose of any items directly in the attributes list so they are not disposed of twice.
      super.scopeDestroyed();
   }
}

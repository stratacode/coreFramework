import sc.obj.ScopeDefinition;
import sc.obj.BaseScopeContext;

import sc.lang.html.Window;

import sc.dyn.DynUtil;

import java.util.HashMap;

public class WindowScopeContext extends BaseScopeContext {
   // The integer identifying this window in this session - from 0 as the first page in the session
   public int windowId;

   // The javascript window object
   Window window;

   SyncWaitListener waitingListener;

   public WindowScopeContext(int windowId, Window window) {
      this.windowId = windowId;
      this.window = window;
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
}

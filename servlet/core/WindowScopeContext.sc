import sc.obj.ScopeDefinition;
import sc.obj.BaseScopeContext;

import sc.lang.html.Window;

import sc.dyn.DynUtil;

import java.util.HashMap;
import sc.util.PerfMon;

import sc.obj.CurrentScopeContext;

import sc.sync.SyncManager;
import sc.sync.SyncManager.SyncContext;

public class WindowScopeContext extends BaseScopeContext {
   // The integer identifying this window in this session - from 0 as the first page in the session
   public int windowId;

   // The javascript window object
   Window window;

   String origHref;

   SyncWaitListener waitingListener;

   long lastRequestTime = -1;

   List<IPage> currentPages;

   // The server API was used to close this session
   boolean windowClosedByServer = false;

   boolean windowRemoved = false;

   boolean sessionExpired = false;;

   public WindowScopeContext(int windowId, Window window) {
      this.windowId = windowId;
      this.window = window;
      this.origHref = window.location.href;
   }

   public ScopeDefinition getScopeDefinition() {
      return WindowScopeDefinition;
   }

   public String getId() {
      return "window:" + String.valueOf(windowId);
   }

   public Window getWindow() {
      return window;
   }

   public boolean isCurrent() {
      Context ctx = Context.getCurrentContext();
      return ctx != null && ctx.windowCtx == this;
   }

   public String getTraceId() {
      StringBuilder sb = new StringBuilder();
      sb.append(super.getTraceId());
      if (window != null) {
         sb.append(" href=" + window.location.href);
      }
      return sb.toString();
   }

   public String getTraceInfo() {
      StringBuilder sb = new StringBuilder();
      sb.append(super.getTraceInfo());
      if (lastRequestTime != -1)
         sb.append("lastRequest = " + PerfMon.getTimeDelta(lastRequestTime));
      if (window != null) {
         if (window.innerWidth != Window.DefaultWidth)
            sb.append("innerWidth = " + window.innerWidth + "\n");
         if (window.innerHeight != Window.DefaultHeight)
            sb.append("innerHeight = " + window.innerHeight + "\n");
      }
      if (waitingListener != null) {
         SyncWaitListener listener = waitingListener;
         if (listener.waiting)
            sb.append("waitingThread = " + listener.threadName + " for: " + PerfMon.getTimeDelta(listener.waitStartTime));
         else {
            if (listener.closed)
               sb.append("window closed");
            else
               sb.append("window open - not listening for sync events");
            if (listener.threadName != null) {
               sb.append(" - " + (listener.lastWakeTime == -1 ? " no events delivered " : "last event sent: " + PerfMon.getTimeDelta(listener.lastWakeTime)) + " last thread: " + listener.threadName);
            }
         }
         sb.append("\n");
      }
      return sb.toString();
   }

   public void removeScopeContext() {
      if (windowRemoved)
         return;
      windowRemoved = true;

      // If we have navigated away from this page, restore the original href without a change event so that if we go back to it, we can
      // navigate away again. Otherwise, the href property does not change and does not get sync'd back
      if (window != null) {
         // Call the closeListeners
         window.windowClosed(sessionExpired);

         if (window.location != null && !DynUtil.equalObjects(origHref, window.location.href)) {
            SyncContext syncCtx = (SyncContext) getValue(SyncManager.SC_SYNC_CONTEXT_SCOPE_KEY);
            if (syncCtx != null)
               syncCtx.removePreviousValue(window.location, "href");
         }
      }

      String scopeContextName = (String) getValue("scopeContextName");
      if (scopeContextName != null) {
         if (!CurrentScopeContext.remove(scopeContextName))
            System.err.println("*** Failed to remove CurrentScopeContext for scopeContextName: " + scopeContextName);
      }

      if (currentPages != null) {
         List<IPage> removePages = currentPages;
         currentPages = null;
         for (int pix = 0; pix < removePages.size(); pix++) {
            IPage removePage = removePages.get(pix);
            List<CurrentScopeContext> curScopes = removePage.getCurrentScopeContexts();
            if (curScopes != null) {
               for (int csx = 0; csx < curScopes.size(); csx++) {
                  CurrentScopeContext curScope = curScopes.get(csx);
                  if (curScope.scopeContexts.contains(this)) {
                     curScopes.remove(csx);
                     csx--;
                  }
               }
            }
         }
      }
   }

   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("window:");
      sb.append(windowId % Context.WindowIdSeparatorScale);
      return sb.toString();
   }

   public void closeScopeContext() {
      windowClosedByServer = true;
   }

   public Object getValue(String name) {
      Object res = super.getValue(name);
      if (res != null)
         return res;
      if (name != null && name.equals("window"))
         return window;
      return null;
   }
}

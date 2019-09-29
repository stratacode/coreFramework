import sc.obj.ScopeDefinition;
import sc.obj.BaseScopeContext;

import sc.lang.html.Window;

import sc.dyn.DynUtil;

import java.util.HashMap;
import sc.util.PerfMon;

public class WindowScopeContext extends BaseScopeContext {
   // The integer identifying this window in this session - from 0 as the first page in the session
   public int windowId;

   // The javascript window object
   Window window;

   SyncWaitListener waitingListener;

   long lastRequestTime = -1;

   public WindowScopeContext(int windowId, Window window) {
      this.windowId = windowId;
      this.window = window;
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
}

import sc.obj.IScopeChangeListener;
import sc.obj.ICodeUpdateListener;
import sc.dyn.DynUtil;

class SyncWaitListener implements IScopeChangeListener, ICodeUpdateListener {
   Context ctx;
   boolean waiting = false;
   boolean replaced = false;
   boolean closed = false;

   // debug only
   String threadName;
   long waitStartTime;
   long lastWakeTime;

   SyncWaitListener(Context ctx) {
      this.ctx = ctx;
      threadName = DynUtil.getCurrentThreadString();
      waitStartTime = System.currentTimeMillis();
      lastWakeTime = -1;
   }

   synchronized void scopeChanged() {
      this.notify();
   }

   public String toString() {
      return threadName + (waiting ? " waiting" : "");
   }

   // So we get codeUpdates
   public synchronized void codeUpdated() {
      this.notify();
   }
}

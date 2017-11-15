import sc.obj.IScopeChangeListener;
import sc.obj.ICodeUpdateListener;

class SyncWaitListener implements IScopeChangeListener, ICodeUpdateListener {
   Context ctx;
   String threadName; // debug only
   boolean waiting = false;
   SyncWaitListener(Context ctx) {
      this.ctx = ctx;
      threadName = PageDispatcher.getCurrentThreadString();
   }

   synchronized void scopeChanged() {
      this.notify();
   }

   public String toString() {
      return PageDispatcher.getSessionTraceInfo(ctx.session) + ": " + threadName + (waiting ? " waiting" : "");
   }

   // So we get codeUpdates
   public synchronized void codeUpdated() {
      this.notify();
   }
}

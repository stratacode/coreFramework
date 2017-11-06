import sc.obj.IScopeChangeListener;

class SyncWaitListener implements IScopeChangeListener {
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
}

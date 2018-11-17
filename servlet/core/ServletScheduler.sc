import sc.dyn.DynUtil;
import sc.dyn.ScheduledJob;
import sc.obj.CurrentScopeContext;

class ServletScheduler implements sc.dyn.IScheduler {
   static Object invokeNextRequestLock = new Object();
   static ArrayList<ScheduledJob> toInvokeNextRequest = new ArrayList<ScheduledJob>();

   public void invokeLater(Runnable runnable, int priority) {
      ScheduledJob sj = new ScheduledJob();
      sj.toInvoke = runnable;
      sj.priority = priority;
      sj.curScopeCtx = CurrentScopeContext.getThreadScopeContext();
      Context ctx = Context.getCurrentContext();

      // No current request - schedule this job to run before the next one
      if (ctx == null) {
         synchronized(invokeNextRequestLock) {
            toInvokeNextRequest.add(sj);
         }
         return;
      }

      ctx.invokeLater(sj);
   }

   void execLaterJobs() {
      Context ctx = Context.getCurrentContext();
      ctx.execLaterJobs();
   }

   static void init() {
      if (DynUtil.frameworkScheduler == null)
         DynUtil.frameworkScheduler = new ServletScheduler();
   }

   // TODO: is this still needed?
   static void execBeforeRequestJobs() {
      Context ctx = Context.getCurrentContext();
      if (ctx != null) {
         ArrayList<ScheduledJob> toExecList = null;
         synchronized(invokeNextRequestLock) {
            if (toInvokeNextRequest.size() > 0)
               toExecList = (ArrayList<ScheduledJob>) toInvokeNextRequest.clone();
            toInvokeNextRequest.clear();
         }
         if (toExecList != null) {
            // Adds all pending jobs to the invoke later list
            for (ScheduledJob sj:toExecList) {
               ctx.invokeLater(sj);
            }

            // Runs the entries in the list
            ctx.execLaterJobs();
         }
      }
   }

   public boolean hasPendingJobs() {
      synchronized(invokeNextRequestLock) {
         return toInvokeNextRequest.size() > 0;
      }
   }
}

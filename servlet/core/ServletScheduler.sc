import sc.dyn.DynUtil;
import sc.dyn.ScheduledJob;
import sc.obj.CurrentScopeContext;

class ServletScheduler implements sc.dyn.IScheduler {
   static Object invokeNextRequestLock = new Object();
   static ArrayList<ScheduledJob> toInvokeNextRequest = new ArrayList<ScheduledJob>();

   public ScheduledJob invokeLater(Runnable runnable, int priority) {
      ScheduledJob sj = new ScheduledJob();
      sj.toInvoke = runnable;
      sj.priority = priority;
      sj.curScopeCtx = CurrentScopeContext.getThreadScopeContext();
      Context ctx = Context.getCurrentContext();

      // No current request - schedule this job to run before the next one
      // TODO: why do we need this?  It seems like a dangerous thing if code were to hit this code path and be running
      // jobs in the wrong context.
      if (ctx == null) {
         System.err.println("Warning: scheduling servlet job with no request context - will invoke on next request");
         synchronized(invokeNextRequestLock) {
            toInvokeNextRequest.add(sj);
         }
      }
      else
         ctx.invokeLater(sj);
      return sj;
   }

   public boolean clearInvokeLater(ScheduledJob job) {
      Context ctx = Context.getCurrentContext();
      if (ctx == null) {
         synchronized(invokeNextRequestLock) {
            return toInvokeNextRequest.remove(job);
         }
      }
      else
         return ctx.clearInvokeLater(job);
   }

   void execLaterJobs(int minPriority, int maxPriority) {
      Context ctx = Context.getCurrentContext();
      if (ctx == null) {
         System.out.println("Warning - ServletScheduler.execLaterJobs called with no current context - session expired or invalidated");
         return;
      }
      ctx.execLaterJobs(minPriority, maxPriority);
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

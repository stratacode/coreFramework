import sc.dyn.DynUtil;
import sc.dyn.ScheduledJob;
import java.awt.EventQueue;
import javax.swing.SwingUtilities;
import sc.dyn.IScheduler;
import sc.obj.CurrentScopeContext;

public class SwingScheduler implements sc.dyn.IScheduler {

   static Object invokeNextLock = new Object();
   private static ArrayList<ScheduledJob> toInvokeNext = new ArrayList<ScheduledJob>();
   private static boolean runScheduled = false;

   public static void init() {
      if (DynUtil.frameworkScheduler == null)
         DynUtil.frameworkScheduler = new SwingScheduler();
   }
   public void execLaterJobs(int minPriority, int maxPriority) {
      ScheduledJob.runJobList(toInvokeNext, minPriority, maxPriority);
   }

   public ScheduledJob invokeLater(Runnable runnable, int priority) {
      ScheduledJob sj = new ScheduledJob();
      sj.toInvoke = runnable;
      sj.priority = priority;
      sj.curScopeCtx = CurrentScopeContext.getThreadScopeContext();

      synchronized(invokeNextLock) {
         ScheduledJob.addToJobList(toInvokeNext, sj);
         // Used to only schedule a job when not on the event thread. but often we use invokeLater to stage something
         // to run after all of the bindings have fired. In that case, this runs the job too early (and may run tasks
         // more often than necessary)
         //if (EventQueue.isDispatchThread())
         //   execLaterJobs(IScheduler.NO_MIN, IScheduler.NO_MAX);
         if (!runScheduled) {
            runScheduled = true;
            SwingUtilities.invokeLater(new Runnable() {
               public void run() {
                  synchronized (invokeNextLock) {
                     runScheduled = false;
                     execLaterJobs(IScheduler.NO_MIN, IScheduler.NO_MAX);
                  }
               }
            });
         }
      }
      return sj;
   }

   public boolean clearInvokeLater(ScheduledJob job) {
      synchronized(invokeNextLock) {
         return ScheduledJob.removeJobFromList(toInvokeNext, job);
      }
   }

   public boolean hasPendingJobs() {
      synchronized(invokeNextLock) {
         return toInvokeNext.size() > 0;
      }
   }
}

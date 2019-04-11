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

   public void invokeLater(Runnable runnable, int priority) {
      ScheduledJob sj = new ScheduledJob();
      sj.toInvoke = runnable;
      sj.priority = priority;
      sj.curScopeCtx = CurrentScopeContext.getThreadScopeContext();

      synchronized(invokeNextLock) {
         ScheduledJob.addToJobList(toInvokeNext, sj);
         if (EventQueue.isDispatchThread())
            execLaterJobs(IScheduler.NO_MIN, IScheduler.NO_MAX);
         else if (!runScheduled) {
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
   }

   public boolean hasPendingJobs() {
      synchronized(invokeNextLock) {
         return toInvokeNext.size() > 0;
      }
   }
}

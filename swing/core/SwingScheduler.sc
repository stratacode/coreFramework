import sc.dyn.DynUtil;
import sc.dyn.ScheduledJob;
import javax.swing.SwingUtilities;

public class SwingScheduler implements sc.dyn.IScheduler {

   public void invokeLater(Runnable runnable, int priority) {
      SwingUtilities.invokeLater(runnable);
   }

   public static void init() {
      if (DynUtil.frameworkScheduler == null)
         DynUtil.frameworkScheduler = new SwingScheduler();
   }

   public void execLaterJobs() {
      System.err.println("*** SwingScheduler does not support execLaterJobs");
   }
}

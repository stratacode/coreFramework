import sc.dyn.DynUtil;

class ServletScheduler implements sc.dyn.IScheduler {
   static class ScheduledJob {
      Runnable toInvoke;
      int priority;
   }

   public void invokeLater(Runnable runnable, int priority) {
      ScheduledJob sj = new ScheduledJob();
      sj.toInvoke = runnable;
      sj.priority = priority;
      Context ctx = Context.getCurrentContext();
      if (ctx == null) {
         System.out.println("ServletScheduler - skipping job sent when not in context of a request");
         return;
      }

      if (ctx.toInvokeLater == null)
         ctx.toInvokeLater = new ArrayList<ScheduledJob>();
      int i;
      int len = ctx.toInvokeLater.size();
      for (i = 0; i < len; i++) {
         ScheduledJob oldSJ = ctx.toInvokeLater.get(i);
         if (oldSJ.priority < priority)
            break;
      }
      if (i == len)
         ctx.toInvokeLater.add(sj);
      else
         ctx.toInvokeLater.add(i, sj);
   }

   void execLaterJobs() {
      Context ctx = Context.getCurrentContext();
      ctx.execLaterJobs();
   }

   static void init() {
      if (DynUtil.frameworkScheduler == null)
         DynUtil.frameworkScheduler = new ServletScheduler();
   }
}

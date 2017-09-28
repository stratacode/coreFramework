import javax.swing.Timer;

@sc.swing.MainInit
object AutoTestMain implements java.awt.event.ActionListener {
   int autoTestInterval = 1000;
   int exitOnIdleDelay = 5000;
   Timer testTimer = new Timer(autoTestInterval, this);
   {
      testTimer.start();
   }
   void actionPerformed(java.awt.event.ActionEvent e) {
      long now = System.currentTimeMillis();
      if (now - sc.swing.SwingUtil.lastUserActionTime > exitOnIdleDelay)
         System.exit(0);
   }
}
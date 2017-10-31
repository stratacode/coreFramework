import sc.type.PTypeUtil;

public class Transition implements Runnable {
   public double duration = 1.0; 
   public int numSteps = 10;
   public boolean inTransition = false;
   public int currentStep = 0;
   public boolean enabled = true; // TODO: set this to false for the server?

   public void startTransition() {
      if (enabled) {
         inTransition = true;
         currentStep = 0;
         PTypeUtil.addScheduledJob(this, (long)(delay * 1000), false);
      }
      else {
         endTransition();
      }
   }

   public double getDelay() {
      return duration / numSteps;
   }

   public void transitionStep() {
      currentStep++;
      if (currentStep < numSteps) {
         PTypeUtil.addScheduledJob(this, (long)(delay * 1000), false);
      }
      else {
         endTransition();
      }
   }

   public void run() {
      transitionStep();
   }

   public void endTransition() {
      inTransition = false;
   }

   public boolean closeEquals(double a, double b) {
      return Math.abs(a - b) < 1.0e-7;
   }
}


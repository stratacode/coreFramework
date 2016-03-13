import sc.type.PTypeUtil;

public class Transition implements Runnable {
   public double duration = 1.0; 
   public int numSteps = 10;
   public boolean inTransition = false;
   public int currentStep = 0;
   public boolean enabled = PTypeUtil.isInvokeLaterSupported();

   public void startTransition() {
      if (enabled) {
         inTransition = true;
         currentStep = 0;
         PTypeUtil.invokeLater(this, (long)(delay * 1000));
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
         PTypeUtil.invokeLater(this, (long)(delay * 1000));
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


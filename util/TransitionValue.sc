public class TransitionValue extends Transition {
   public double from;
   public double to;
   public double cur := inTransition ? (from - to) / numSteps * currentStep + from : to;

   to =: startTransition();

   public void startTransition() {
      if (closeEquals(to, from)) {
         if (inTransition)
            endTransition();
         return;
      }
      if (!inTransition)
         super.startTransition();
   }

   public void endTransition() {
      super.endTransition();
      from = to;
   }
}


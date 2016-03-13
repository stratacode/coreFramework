public class TransitionPoint extends Transition {
   public double fromX, fromY;
   public double toX, toY;
   public double curX := inTransition ? (fromX - toX) / numSteps * currentStep + fromX : toX;
   public double curY := inTransition ? (fromY - toY) / numSteps * currentStep + fromY : toY;

   toX =: startTransition();
   toY =: startTransition();

   public void startTransition() {
      if (closeEquals(toX, fromX) && closeEquals(toY,fromY)) {
         if (inTransition)
            endTransition();
         return;
      }
      if (!inTransition)
         super.startTransition();
   }
   public void endTransition() {
      super.endTransition();
      fromX = toX;
      fromY = toY;
   }
}


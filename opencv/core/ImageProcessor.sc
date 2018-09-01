abstract class ImageProcessor {
   Mat inMat;
   int inChangeCt;
   int outChangeCt;

   boolean needsRefresh;

   inMat =: scheduleRefresh();
   inChangeCt =: scheduleRefresh();

   public void scheduleRefresh() {
      if (!needsRefresh) {
         needsRefresh = true;
         DynUtil.invokeLater(new Runnable() {
            public void run() {
               needsRefresh = false;
               if (inMat != null) {
                  if (refresh())
                     outChangeCt++;
               }
            }
         }, 0);
      }
   }

   abstract boolean refresh();
}
class ImageBlur extends ImageProcessor {
   int blurSize = 5;
   Mat outMat;

   inChangeCt =: scheduleRefresh();
   blurSize =: scheduleRefresh();
   inMat =: scheduleRefresh();

   public boolean refresh() {
      if (blurSize > 1) {
         outMat = new Mat();
         Imgproc.blur(inMat, outMat, new Size(blurSize, blurSize));
      }
      else {
         outMat = inMat;
      }
      return true;
   }
}

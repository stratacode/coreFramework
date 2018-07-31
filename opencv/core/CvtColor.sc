class CvtColor extends ImageProcessor {
   Mat outMat = new Mat();

   int format = -1;

   format =: scheduleRefresh();

   public void refresh() {
      if (format == -1)
         throw new IllegalArgumentException("Must set format of CvtColor");
      Imgproc.cvtColor(inMat, outMat, format);
   }
}
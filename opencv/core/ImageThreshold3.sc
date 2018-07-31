class ImageThreshold3 extends ImageProcessor {
   Mat outMat;

   double start1 = 0, end1 = 180.0, start2 = 0, end2 = 112, start3 = 0, end3 = 256;

   start1 =: scheduleRefresh();
   start2 =: scheduleRefresh();
   start3 =: scheduleRefresh();
   end1 =: scheduleRefresh();
   end2 =: scheduleRefresh();
   end3 =: scheduleRefresh();

   String label1, label2, label3;

   public void refresh() {
      Mat res = new Mat();
      System.out.println("*** Threshold: " + label1 + ":" + start1 + "-" + end1 + " " + label2 + ": " + start2 + "-" + end2 + " " + label3 + ": " + start3 + "-" + end3);
      Scalar minValues = new Scalar(start1, start2, start3);
      Scalar maxValues = new Scalar(end1, end2, end3);
      Mat mask = new Mat();
      Core.inRange(inMat, minValues, maxValues, mask);
      inMat.copyTo(res, mask);
      outMat = res;
      outChangeCt++;
   }
}
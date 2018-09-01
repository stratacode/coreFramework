class ErodeDilateFilter extends ImageProcessor {
   Mat outMat;
   boolean doDilate = true, doErode = true;

   int dilateSize = 8;
   int erodeSize = 4;

   doDilate =: scheduleRefresh();
   doErode =: scheduleRefresh();
   dilateSize =: scheduleRefresh();
   erodeSize =: scheduleRefresh();

   public boolean refresh() {
      if (doDilate)
         System.out.println("*** dialate: " + dilateSize);
      if (doErode)
         System.out.println("*** erode: " + erodeSize);
      if (!doDilate && !doErode)
         System.out.println("*** dialate+erode disabled");

      Mat src = inMat;

      if (doErode) {
         Mat tempMat = new Mat();
         Imgproc.erode(src, tempMat, new Mat(), new Point(-1, -1), erodeSize);
         src = tempMat;
      }
      if (doDilate) {
         Mat tempMat = new Mat();
         Imgproc.dilate(src, tempMat, new Mat(), new Point(-1, -1), dilateSize);
         src = tempMat;
      }
      outMat = src;
      return true;
   }

}
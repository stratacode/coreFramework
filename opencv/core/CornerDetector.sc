class CornerDetector {
   Mat cornerInImg;
   Mat cornerOutImg = new Mat();

   // Corner parameters
   int blockSize = 6;
   int blockApertureSize = 5; // sobel aperture size for corners
   double k = 0.009;

   int refreshCt;

   blockSize =: refreshCorner();
   blockApertureSize =: refreshCorner();
   k =: refreshCorner();

   public void refreshCorner() {
      if (cornerInImg.cols() > 0) {
         Mat origImgGreyFloat = new Mat();
         cornerInImg.convertTo(origImgGreyFloat, CvType.CV_32FC1);
         Mat resFloat = new Mat();
         if ((blockApertureSize & 1) == 0)
            blockApertureSize++;
         if (blockApertureSize > 31)
            blockApertureSize = 31;
         System.out.println("** Refreshing - corner with sobel: " + blockApertureSize + ", blockSize: " + blockSize + ", k: " +  k);
         Imgproc.cornerHarris(origImgGreyFloat, resFloat, blockSize, blockApertureSize, k);
         resFloat.convertTo(cornerOutImg, CvType.CV_8UC1);
      }
      //Bind.sendChangedEvent(this, "cornerOutImg");
      refreshCt++;
   }
}


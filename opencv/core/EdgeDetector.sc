class EdgeDetector extends ImageProcessor {
   Mat outMat = new Mat();

   double minEdgeVal = 42; // intensity gradient below minVal - definitely not an edge
   double maxEdgeVal = 72;  // intensity gradient above maxVal - definitely an edge - in between based on connectivity
   int apertureSize = 3; // size of sobel kernel used for finding gradients

   minEdgeVal =: scheduleRefresh();
   maxEdgeVal =: scheduleRefresh();
   apertureSize =: scheduleRefresh();

   public void computeEdgeImage() {
      System.out.println("*** Compute edge image running:");
      if ((apertureSize & 1) == 0)
         apertureSize++;
      if (apertureSize > 31)
         apertureSize = 31;
      Imgproc.Canny(inMat, outMat, minEdgeVal, maxEdgeVal, apertureSize, false);

      ImgUtil.dumpMinMax(outMat, "after canny");
   }

   public void refresh() {
      System.out.println("** Refreshing - edge: " + minEdgeVal + ", " + maxEdgeVal + " sobel size: " + apertureSize);
      computeEdgeImage();
      //refreshFeaturePoints();

      // Because computeEdgeImage does not change the value of the edgeOutImg field, need to send this change event
      // TODO: We've changed the contents of Mat, but not the instance so the binding will not receive the
      // change event.   We could implement IChangeable in a wrapper class for Mat and call a 'sendEvent' on the
      // Mat itself.  For now, the
      //Bind.sendChangedEvent(this, "edgeOutImg");
   }
}

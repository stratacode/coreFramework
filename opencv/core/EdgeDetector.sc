class EdgeDetector {
   Mat edgeInImg;
   Mat edgeOutImg = new Mat();

   double minEdgeVal = 42; // intensity gradient below minVal - definitely not an edge
   double maxEdgeVal = 72;  // intensity gradient above maxVal - definitely an edge - in between based on connectivity
   int apertureSize = 3; // size of sobel kernel used for finding gradients

   int refreshCt = 0;

   minEdgeVal =: refreshEdge();
   maxEdgeVal =: refreshEdge();
   apertureSize =: refreshEdge();

   public void computeEdgeImage() {
      System.out.println("*** Compute edge image running:");
      if ((apertureSize & 1) == 0)
         apertureSize++;
      if (apertureSize > 31)
         apertureSize = 31;
      Imgproc.Canny(edgeInImg, edgeOutImg, minEdgeVal, maxEdgeVal, apertureSize, false);

      ImgUtil.dumpMinMax(edgeOutImg, "after canny");
   }

   public void refreshEdge() {
      System.out.println("** Refreshing - edge: " + minEdgeVal + ", " + maxEdgeVal + " sobel size: " + apertureSize);
      computeEdgeImage();
      //refreshFeaturePoints();

      // Because computeEdgeImage does not change the value of the edgeOutImg field, need to send this change event
      // TODO: We've changed the contents of Mat, but not the instance so the binding will not receive the
      // change event.   We could implement IChangeable in a wrapper class for Mat and call a 'sendEvent' on the
      // Mat itself.  For now, the
      //Bind.sendChangedEvent(this, "edgeOutImg");
      refreshCt++;
   }
}

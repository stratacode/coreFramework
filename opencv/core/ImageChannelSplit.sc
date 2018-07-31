class ImageChannelSplit extends ImageProcessor {
   List<Mat> outChannels;

   void refresh() {
      List<Mat> channels = new ArrayList<Mat>();
      Core.split(inMat, channels);
      outChannels = channels;
   }
}
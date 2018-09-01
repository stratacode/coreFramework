class ImageChannelSplit extends ImageProcessor {
   List<Mat> outChannels;

   boolean refresh() {
      List<Mat> channels = new ArrayList<Mat>();
      Core.split(inMat, channels);
      outChannels = channels;
      return true;
   }
}
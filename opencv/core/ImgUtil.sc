class ImgUtil {
   public static double MAX_DOUBLE = Double.MAX_VALUE;
   public static boolean verboseImage;

   public static void dumpMinMax(Mat mat, String message) {
      if (!verboseImage)
         return;
      int height = mat.rows();
      int width = mat.cols();
      int channels = mat.channels();
      double[] minVals = new double[channels];
      double[] maxVals = new double[channels];
      for (int i = 0; i < channels; i++) {
         minVals[i] = MAX_DOUBLE;
      }
      for (int c = 0; c < width; c++) {
         for (int r = 0; r < height; r++) {
            double[] vals  = mat.get(r, c);
            if (vals == null)
               System.err.println("*** no value for:" + r + ": " + c);
            else {
               for (int i = 0; i < channels; i++) {
                  if (vals[i] > maxVals[i])
                     maxVals[i] = vals[i];
                  if (vals[i] < minVals[i])
                     minVals[i] = vals[i];
               }
            }
         }
      }
      StringBuilder sb = new StringBuilder();
      sb.append(message);
      sb.append(":  min: ");
      for (int i = 0; i < channels; i++) {
         if (i != 0)
            sb.append(",");
         sb.append(minVals[i]);
      }
      sb.append("\n");
      sb.append("max: ");
      for (int i = 0; i < channels; i++) {
         if (i != 0)
            sb.append(",");
         sb.append(maxVals[i]);
      }
      System.out.println(sb);
   }
}
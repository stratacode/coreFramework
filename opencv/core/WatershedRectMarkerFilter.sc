class WatershedRectMarkerFilter extends ImageProcessor {
   Mat markerTagImg;
   Mat markerImg;
   Mat markerResult;
   Mat outImg;
   Mat contourImg;

   //List<List<Rect>> markerRects = new ArrayList<List<Rect>>();
   int[][][] featurePts = {
      // define feature as list of markers
      //{startx,starty},{endx,endy} for each marker
      {
        // background
        {0,0},{50,50},
        {950,0},{1000,50},
        {0,950},{50,1000},
        {950,950},{1000,1000}
      },
      {
        // foot
        {400,400}, {600,850}
      },
      {
        // paper
        {175,175}, {250,250},
        {725,725}, {800,800}
      }
   };

   List<Scalar> markerColors = {
                                new Scalar(200,200,200),  // unused?
                                new Scalar(35, 55, 100), // background
                                new Scalar(200, 110, 150), // foot
                                new Scalar(230, 230, 230) // paper
                              };

   boolean refresh() {
      if (inMat == null)
         return false;
      int w = inMat.width();
      int h = inMat.height();
      Size inSize = new Size(w, h);
      markerTagImg = Mat.zeros(inSize, CvType.CV_32S);

      int numFeatures = featurePts.length;

      for (int f = 0; f < numFeatures; f++) {
         int[][] rlpts = featurePts[f];
         Scalar markerValue = new Scalar(f+1);
         for (int rl = 0; rl < rlpts.length; rl += 2) {
            int[] start = rlpts[rl];
            int[] end = rlpts[rl+1];
            Imgproc.rectangle(markerTagImg, new Point(convertX(start[0]), convertY(start[1])),
                                            new Point(convertX(end[0]), convertY(end[1])),
                                            markerValue, Core.FILLED);
         }
      }

      markerImg = convertTagToColors(markerTagImg, markerColors);

      Imgproc.watershed(inMat, markerTagImg);

      outImg = convertTagToColors(markerTagImg, markerColors);

      List<Mat> masks = convertTagToMasks(markerTagImg, numFeatures);
      contourImg = Mat.zeros(inSize, CvType.CV_8UC3);
      buildContourImage(contourImg, masks, markerColors);

      return true;
   }

   private static void buildContourImage(Mat contourImg, List<Mat> masks, List<Scalar> markerColors) {
      for (int i = 0; i < masks.size(); i++) {
         Mat mask = masks.get(i);
         Mat hierarchy = new Mat();
         System.out.println("*** Starting findContours");
         List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
         Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);

         Imgproc.drawContours(contourImg, contours, -1, markerColors.get(i+1), 10);
      }
   }

   public static Mat convertTagToColors(Mat tagImg, List<Scalar> colors) {
      int w = tagImg.width();
      int h = tagImg.height();
      Size inSize = new Size(w, h);
      Mat resImg = Mat.zeros(inSize, CvType.CV_8UC3);
      byte[] resData = new byte[(int) (resImg.total()*resImg.channels())];
      resImg.get(0, 0, resData);

      int[] markerTagData = new int[(int) tagImg.total() * tagImg.channels()];
      tagImg.get(0, 0, markerTagData);

      int numMarkers = colors.size();
      for (int y = 0; y < h; y++) {
         for (int x = 0; x < w; x++) {
            int soff = y * w + x;
            int coff = soff * 3;
            int markerVal = markerTagData[soff];

            if (markerVal > 0 && markerVal < numMarkers) {
               resData[coff + 0] = (byte) colors.get(markerVal).val[0];
               resData[coff + 1] = (byte) colors.get(markerVal).val[1];
               resData[coff + 2] = (byte) colors.get(markerVal).val[2];
            }
            else {
               resData[coff + 0] = 0;
               resData[coff + 1] = 0;
               resData[coff + 2] = 0;
            }
         }
      }
      resImg.put(0, 0, resData);
      return resImg;
   }

   public static List<Mat> convertTagToMasks(Mat tagImg, int numMarkers) {
      int w = tagImg.width();
      int h = tagImg.height();
      Size inSize = new Size(w, h);
      Mat resImg = Mat.zeros(inSize, CvType.CV_8UC3);
      byte[] resData = new byte[(int) (resImg.total()*resImg.channels())];
      resImg.get(0, 0, resData);

      List<Mat> masks = new ArrayList<Mat>();

      int[] markerTagData = new int[(int) tagImg.total() * tagImg.channels()];
      tagImg.get(0, 0, markerTagData);

      List<byte[]> maskDataList = new ArrayList<byte[]>();

      for (int i = 0; i < numMarkers; i++) {
         Mat tagMat = Mat.zeros(inSize, CvType.CV_8U);
         byte[] maskData = new byte[w*h];
         masks.add(tagMat);
         maskDataList.add(maskData);
      }

      for (int y = 0; y < h; y++) {
         for (int x = 0; x < w; x++) {
            int off = y * w + x;
            int markerVal = markerTagData[off];

            if (markerVal > 0 && markerVal < numMarkers) {
               byte[] maskData = maskDataList.get(markerVal-1);
               maskData[off] = 1;
            }
         }
      }

      for (int i = 0; i < numMarkers; i++) {
         Mat tagMat = masks.get(i);
         byte[] maskData = maskDataList.get(i);
         tagMat.put(0, 0, maskData);
      }
      return masks;
   }

   double convertX(int val) {
      return val/1000.0 * inMat.width();
   }

   double convertY(int val) {
      return val/1000.0 * inMat.height();
   }
}
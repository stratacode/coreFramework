class ImageDisplay extends JLabel {
   Mat inputMat;
   Mat dispMat = new Mat();
   BufferedImage dispBuf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

   int displayWidth, displayHeight;

   inputMat =: refresh();

   int refreshTrigger = 0;
   refreshTrigger =: refresh();

   size := SwingUtil.dimension(displayWidth, displayHeight);

   object icon extends ImageIcon {
      image := dispBuf;
   }

   void refresh() {
      if (inputMat.cols() > 0) {
         Imgproc.resize(inputMat, dispMat, new Size(displayWidth, displayHeight));
         dispBuf = ImgSwingUtil.toBufferedImage(dispMat);
      }
      setIcon(icon);
      updateUI();
   }
}


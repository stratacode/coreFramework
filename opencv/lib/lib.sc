import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Size;
import org.opencv.core.Rect;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

opencv.lib {
   String openCVLibDir =  "/usr/local/opt/opencv/share/OpenCV/java";
   void start() {
      classPath = sc.util.FileUtil.listFiles(openCVLibDir,".*\\.jar");
      /*
      if (activated && !disabled) {
          // TODO: by doing this here, it requires the dynamic runtime - eventually move
          // this into an @MainInit class that runs before any application code so it would
          // work for a compiled application without the dynamic runtime.
          sc.layer.LayerUtil.addLibraryPath(openCVLibDir);
          System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
      }
      */
   }
}


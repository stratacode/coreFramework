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

// To install on mac os X - follow the instructions for installing with homebrew and java enabled
//   - brew edit opencv
//           - find line with:
//               -DBUILD_opencv_java=OFF
//              and change it to:
//               -DBUILD_opencv_java=ON
//   - brew install --build-from-source opencv
// NOTE: Last time I tried this I had problems compiling opencv and just copied the /usr/local/opt/opencv dir from the previous machine.

opencv.lib {
   compiledOnly = true;
   hidden = true;

   codeType = CodeType.Framework;
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


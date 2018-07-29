// Needs to run right at startup so we load the library before anything else happens
@CompilerSettings(createOnStartup=true, startPriority=1000)
@Component
object ImgSwingInit {
   // Use the layer's ok, we can still type in here value of openCVLibDir to initialize this property
   @sc.obj.BuildInit("openCVLibDir")
   public String openCVLibDir;
   void init() {
      try {
         sc.layer.LayerUtil.addLibraryPath(openCVLibDir);
         System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
      }
      catch (Exception exc) {
         System.err.println("*** failed to load openCV native library: " + exc);
      }
   }
}
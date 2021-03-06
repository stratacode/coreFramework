package sc.opengl;

// TODO: consider porting to use https://www.lwjgl.org/ since jogl seems
// not to be supported well anymore 
opengl.lib {
   compiledOnly = true;
   codeType = sc.layer.CodeType.Framework;

   void init() {
      // For now we can't use the open gl layers for js and android but
      // theoretically that could change.
      excludeRuntimes("js", "android", "gwt");
      addRuntime(null);
   }

   void start() {
      sc.repos.RepositoryPackage pkg = addRepositoryPackage("javaOpenGL", "url", "http://www.stratacode.com/packages/opengl.zip", true, false);
      if (!disabled) {
         String openGLDir = sc.util.FileUtil.concat(pkg.installedRoot, "lib");
         classPath = sc.util.FileUtil.listFiles(openGLDir,".*\\.jar");
         //classPath=openGLDir + "jogl.all.jar:" + openGLDir + "nativewindow.all.jar:" + 
         //          openGLDir + "gluegen-rt.jar:" + openGLDir + "newt.all.jar";
         /*String path = System.getProperty("java.library.path");
         if (path == null) path = "";
         else path += ":";
         path += openGLDir;
         System.setProperty("java.library.path", path); */
         sc.layer.LayerUtil.addLibraryPath(openGLDir);
         System.out.println("opengl.lib layer updated system library path: " + System.getProperty("java.library.path"));
      }
   }
}

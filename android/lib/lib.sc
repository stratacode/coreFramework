// No package prefix here because we put the "gen" directory in this layer.  It's defined in terms of root

/** 
 * This layer contains basic android support.  It defines the various build options to generate an android project in the sc buildDir.
 * Extends util because it overrides some of the util functionality for scxml.
 *
 * To install: 
 * Install the android sdk in /usr/local/androidsdk
 * verify the link: layers/android/lib link to platform dir you want to use, typically: /usr/local/androidsdk/platforms/android-15
 * verify the link: layers/android/sdk link to platform dir you want to use, typically: /usr/local/androidsdk
 */
android.lib extends util {
   inheritPackage = false;
   // This gets inherited from any sub-layers and tells the system to put the src into a sub-directory of the normal build directory
   buildSrcSubDir = "src";
 
   {
      if (activated) {
         layeredSystem.runtimePrefix = "";
         excludedPaths.add("sdk");
         // Exclude something.xml.d, R.java.d etc.
         excludedFiles.add("\\..*\\.d");
      }
   }

   // Include Android's generated source - R.java in the layer's src path
   srcPath = FileUtil.concat("${buildDir}", "gen") + FileUtil.PATH_SEPARATOR + ".";

   needsIndexRefresh = true; // We add files to the "gen" directory in the pre-build commands.  This tells the layer to flush the cache after each phase

   public void init() {
      createDefaultRuntime("android");

      // Do not load these android classes into the dynamic runtime, just like javascript
      sc.layer.DefaultRuntimeProcessor rtproc = (sc.layer.DefaultRuntimeProcessor) definedRuntime;
      rtproc.setLoadClassesInRuntime(false);

      excludeRuntimes("js", "gwt", "java");

      // These must be in the init phase since we need to see the Prepare file processor before we start the layer in "layeredSystem.needsPhase"
      sc.lang.TemplateLanguage tempLang = new sc.lang.TemplateLanguage();
      tempLang.processTemplate = true; // At build time, generate the xml file
      // For android, xml files are used by the compiler itself so they need to be done
      // before the process phase in prepare.
      tempLang.buildPhase = sc.layer.BuildPhase.Prepare;
      tempLang.resultSuffix = "xml";
      tempLang.useSrcDir = false;
      registerLanguage(tempLang, "scxml");

      sc.lang.TemplateLanguage propLang = new sc.lang.TemplateLanguage();
      tempLang.processTemplate = true;
      tempLang.buildPhase = sc.layer.BuildPhase.Prepare;
      tempLang.resultSuffix = "properties";
      tempLang.useSrcDir = false;
      registerLanguage(tempLang, "scproperties");

      // Recognizes android resources - copies them over to the build directory using a layered merging of paths
      sc.layer.LayerFileProcessor resourceFileProcessor = new sc.layer.LayerFileProcessor();

      resourceFileProcessor.prependLayerPackage = false;
      resourceFileProcessor.useSrcDir = false;
      // Build this before we process Java files
      resourceFileProcessor.buildPhase = sc.layer.BuildPhase.Prepare;

      // Copy these extensions to the output file
      registerFileProcessor(resourceFileProcessor, "xml");
      registerFileProcessor(resourceFileProcessor, "properties");
      registerFileProcessor(resourceFileProcessor, "png");
   }

   //public static String platformTarget = "android-20";
   //public static String buildToolsVersion = "20.0.0";
   public static String platformTarget = "android-19";
   public static String buildToolsVersion = "19.1.0";

   // A list of places to look for ant by default
   public static String[] antPaths = {
      "/usr/local/bin/ant"
   };

   // Directory holding this file has a symbolic link to the android/platforms/android-xxx directory which contains android.jar
   public void start() {
      // TODO: break this out into an ant layer before we integrate the
      // second framework that uses ant.
      String antCommand = System.getenv("ANT_CMD");
      if (antCommand == null) {
         for (String antPath:antPaths) {
            if (new java.io.File(antPath).canExecute()) {
               antCommand = antPath;
               break;
            }
         }
         if (antCommand == null)
            antCommand = "ant";
      }

      String sdkDir = System.getenv("ANDROID_SDK");
      String platformDir;
      if (sdkDir == null) {
         // TODO: replace macosx with something we pull out of the system for linux and windows
         sc.repos.RepositoryPackage pkg = addRepositoryPackage("androidSDK", "url", "http://dl.google.com/android/android-sdk_r23.0.2-macosx.zip", true);
         sdkDir = sc.util.FileUtil.concat(pkg.installedRoot, "android-sdk-macosx");

         if (!(new java.io.File(sdkDir).isDirectory())) {
            disabled = true;
            System.err.println("*** Failed to install android SDK into:  " + sdkDir);
         }
      }
      else {
         if (!(new java.io.File(sdkDir).isDirectory())) {
            disabled = true;
            System.err.println("*** ANDROID_SDK points to: " + sdkDir + " which does not exist");
         }
      }

      // TODO: we should make Layer.java implement IDynObject, then use the layer type here as the dynamic type so we can use
      // fields in the layer to share info across the system.  resolveObject already will resolve the layer from it's name in the
      // dynamic lookup.  There's more work to manage the DynObject from the layer instance.
      System.setProperty("android.sdkDir", sdkDir);
      System.setProperty("android.platformTarget", platformTarget);

      String androidCmd = FileUtil.concat(sdkDir, "tools", "android");
      java.io.File cmdFile = new java.io.File(androidCmd);
      // Need to make the android command executable
      if (!cmdFile.canExecute())
         cmdFile.setExecutable(true, true);

      platformDir = sc.util.FileUtil.concat(sdkDir, "platforms", platformTarget);
      java.io.File platformFile = new java.io.File(platformDir);
      if (!(platformFile.isDirectory())) {
         System.out.println("No directory - " + platformDir + " - updating android SDK for platform target: " + platformTarget + " build tools version: " + buildToolsVersion);
         // -u = noui,  -s = no https, -t = filter  - filters such as android-19, build-tools-19.0.3 etc. do not work.  It does not seem like  
         // it's possible to do this self-install.  So instead we take out the -u option and force the user to deal with it.
         // It looks like android list --extended provides the set of filters that are valid.  Not the --all ones so that limits which packages you can
         // install from the command line.  Hopefully they'll fix it.   For now we'll prompt you...
         //String res = sc.util.FileUtil.exec("yes\n", true, androidCmd, "update", "sdk", "-u", "-s", "-t", "tools,platform-tools,android-19,build-tools-19.0.3");
         System.out.println("Please choose the latest Platform Tools, Build Tools, SDK Platform API Level 19 and the System image armeabi-v7s then install packages, then close the window to continue");
         String res = sc.util.FileUtil.exec(null, false, androidCmd, "update", "sdk", "-s", "-t", "tools,platform-tools");

         if (!(platformFile.isDirectory())) {
            disabled = true;
            System.err.println("Failed to update android SDK - missing directory: " + platformFile + " update command result: " + res);
         }
      }

      // Include android.jar into the external class path.  We can't load classes like java.lang.AutoCloseable 
      externalClassPath = sc.util.FileUtil.concat(platformDir, "android.jar");

      if (!(new java.io.File(externalClassPath).canRead())) {
         throw new IllegalArgumentException("*** Android SDK is missing a readable android.jar at: " + externalClassPath);
      }

      String adbCommand = sc.util.FileUtil.concat(sdkDir, "platform-tools", "adb");

      sc.layer.LayeredSystem system = getLayeredSystem();

      if (disabled) {
         if (activated)
            throw new IllegalArgumentException("Failed to start android.lib layer");
         return;
      }

      // Don't worry about the emulator unless we are running an application
      if (activated) {
         String listAvdResult = sc.util.FileUtil.exec(null, false, androidCmd, "list", "avd");
         int avdIx = listAvdResult == null ? -1 : listAvdResult.indexOf(":");
         if (avdIx == -1)
            throw new IllegalArgumentException("*** Can't list android devices");
         else
            System.out.println("list avd result: " + listAvdResult);

         String nameToken = "Name:";
         int nameIndex = listAvdResult.indexOf(nameToken);
         String deviceName = "SCTest";
         if (nameIndex == -1) {
            System.out.println("Creaing new android emulator device (avd): SCTest:");
            // newline here as the input string answers "no" to the creating device profile question
            String createDeviceResult = sc.util.FileUtil.exec("\n", true, androidCmd, "create", "avd", "--name", "SCTest", 
                                                              "--target", platformTarget, "--abi", "default/armeabi-v7a");
            if (createDeviceResult == null)
               System.err.println("*** Failed to create android test device: SCTest");
         }
         else {
            int endNameIndex = listAvdResult.indexOf("\n", nameIndex);
            if (endNameIndex != -1)
               deviceName = listAvdResult.substring(nameIndex + nameToken.length()+1, endNameIndex).trim();
            else
               System.err.println("*** Error parsing list avd result");
         }

         System.out.println("List running emulators");
         String listDevicesResult = sc.util.FileUtil.exec(null, true, adbCommand, "devices");
         if (listDevicesResult == null)
            System.err.println("*** Failed to run the adb list devices command");
         else {
            String emulatorCmd = sc.util.FileUtil.concat(sdkDir, "tools", "emulator");
            /*
            java.io.File emulatorFile = new java.io.File(emulatorCmd);
            if (!emulatorFile.canExecute())
               emulatorFile.setExecutable(true, true);
            */

            String pattern = "List of devices attached";
            int firstLine = listDevicesResult.indexOf(pattern);
            if (firstLine == -1 || listDevicesResult.substring(firstLine + pattern.length()).trim().length() == 0) {
               System.out.println("Running emulator for: " + deviceName);
               sc.util.FileUtil.fork(null, true, emulatorCmd, "-avd", deviceName);
            }
            else
               System.out.println("Emulator already running: " + listDevicesResult);
         }

         // Copies scrt.jar into the android libs directory
         system.runtimeLibsDir = sc.util.FileUtil.concat(system.buildDir, "libs");
      }

      // First make the gen folder if necessary
      system.addPreBuildCommand(sc.layer.BuildPhase.Prepare, this, "mkdir", "-p", sc.util.FileUtil.concat(system.buildDir, "gen"), sc.util.FileUtil.concat(system.buildDir, "res"));
      // Then run the aapt command which requires it 
      system.addPreBuildCommand(sc.layer.BuildPhase.Process, this, sc.util.FileUtil.concat(sdkDir,"build-tools/" + buildToolsVersion + "/aapt"),
                                "package", "-m", "-J", "gen", "-M", "AndroidManifest.xml", "-S", "res", "-I", externalClassPath);

      system.addPostBuildCommand(sc.layer.BuildPhase.Process, this, adbCommand, "wait-for-device");
      // Change debug to release for the production install or just comment this out and run ant by hand in the build directory
      system.addPostBuildCommand(sc.layer.BuildPhase.Process, this, antCommand, "debug", "install");
   }

   public void validate() {
      if (activated) {
         // Turn off compilation since we need to use android to do this. 
         layeredSystem.options.noCompile = true;
      }
   }
}

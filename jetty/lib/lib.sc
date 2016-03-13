package sc.jetty;

jetty.lib extends log4j {
   compiledOnly = true;

   codeType = sc.layer.CodeType.Framework;
   codeFunction = sc.layer.CodeFunction.Program;

   object servletPkg extends MvnRepositoryPackage {
      url = "mvn://javax.servlet/javax.servlet-api/3.0.1";
   }

   object jettyPkg extends MvnRepositoryPackage {
      url = "mvn://org.eclipse.jetty/jetty-webapp/8.1.17.v20150415";
   }

   object log4jPkg extends MvnRepositoryPackage {
      url = "mvn://org.slf4j/slf4j-log4j12/1.7.0";
   }

   public void init() {
      // Exclude the javascript, android, and gwt runtimes.  All layers which extend this layer explicitly will also be excluded, unless they explicitly include a layer which uses an excluded runtime
      excludeRuntimes("js", "android", "gwt");

      // Jetty requires the default runtime - Server process
      //addRuntime(null);
      addProcess(sc.layer.ProcessDefinition.create("Server"));

      System.setProperty("org.eclipse.jetty.xml.XmlParser.Validating", "false");
   }

   public void start() {
      sc.layer.LayeredSystem system = getLayeredSystem();

      // The web root is the original compiled buildDir, this tells sc to cd there before running any commands
      system.options.runFromBuildDir = true;
      // WEB-INF is not path searchable so the real build dir is the common build dir (doesn't change for dyn layers)
      system.options.useCommonBuildDir = true;

      // Layers web files in the "doc" folder of any downstream layers
      sc.layer.LayerFileProcessor log4jprops = new sc.layer.LayerFileProcessor();

      // Only layers after this one will see this extension
      log4jprops.definedInLayer = this;    
      log4jprops.prependLayerPackage = false;
      log4jprops.useSrcDir = false;

      // Copy this file into the top-level of the buildDir
      system.registerPatternFileProcessor("log4j\\.properties", log4jprops);

      //sc.repos.RepositoryPackage pkg = addRepositoryPackage("jettyLibs", "scp", "vsgit@stratacode.com:/home/vsgit/jettyLibs", false);
      //sc.repos.RepositoryPackage pkg = addRepositoryPackage("jettyLibs", "url", "http://stratacode.com/packages/jettyLibs.zip", true);
      //RepositoryPackage pkg = addRepositoryPackage("mvn://org.eclipse.jetty/jetty-webapp/9.2.11.v20150529");
      //RepositoryPackage pkg = addRepositoryPackage("mvn://org.eclipse.jetty/jetty-webapp/8.1.17.v20150415");
/*
      if (pkg.installedRoot != null && !disabled) {
         //classPath = sc.util.FileUtil.listFiles(pkg.installedRoot,".*\\.jar");
         classPath = pkg.classPath;
      }
*/
   }
}

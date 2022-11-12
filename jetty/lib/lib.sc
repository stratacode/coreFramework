package sc.jetty;

jetty.lib extends servlet.lib, log4j.core {
   compiledOnly = true;
   hidden = true;

   codeType = sc.layer.CodeType.Framework;

   object jettyPkg extends MvnRepositoryPackage {
      //url = "mvn://org.eclipse.jetty/jetty-webapp/8.1.17.v20150415";
      url = "mvn://org.eclipse.jetty/jetty-webapp/9.4.49.v20220914";
   }

   // TODO: currently the CServer class adds Env processing logic so we go through jetty for 
   // managed data sources but we could just configure those directly as components and avoid
   // this whole thing and make them more manageable
   object jettyPlusPkg extends MvnRepositoryPackage {
      url = "mvn://org.eclipse.jetty/jetty-plus/9.4.49.v20220914";
   }

   object jettySchemas extends MvnRepositoryPackage {
      url = "mvn://org.eclipse.jetty.toolchain/jetty-schemas/3.1.RC0";
   }

   log4jPkg {
      url = "mvn://org.slf4j/slf4j-log4j12/1.7.25";
   }

   public void init() {
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

      // Older ways we have configured this package using the APIs, not components
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

// Pull in the log4 layer
log4j.core extends sys.std {
   compiledOnly = true;

   codeType = sc.layer.CodeType.Framework;

   // Log4j complains if we include two incompatible implementation classes and so higher level 
   // packages need to define the impl library.  Here we only pull in the api.

   object slf4jApiPkg extends MvnRepositoryPackage {
      url = "mvn://org.slf4j/slf4j-api/1.7.25";
   }

   // Supplying a default log4j - this can be overridden if frameworks need a specific version (e.g. jetty/lib)
   object log4jPkg extends MvnRepositoryPackage {
      url = "mvn://org.apache.logging.log4j/log4j-core/2.9.1";
   }

   // The resourceFileProcessor layer component is defined in sys.std 
   // and defines standard Java rules for copying src files into the 
   // classpath so they can be loaded  as Java resources.  
   // Some layers treat properties as config files
   // which are put into the build-dir (with some config prefix).  Here
   // we ensure the log4j.properties file is treated as a resource, 
   // by modifying that instance and adding a new file pattern.
   resourceFileProcessor {
      {
         // Treat this file as a resource so it goes in the classpath
         addPatterns("log4j\\.properties", "log4j2\\.properties");
         // TODO: add log4j2.xml and .json here?
      }
   }

   // This method is run when the layer is initialized.  
   public void init() {
      // log4j is automatically excluded from these runtimes
      excludeRuntimes("js", "android", "gwt");
   }
}

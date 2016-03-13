// Pull in the log4 layer
log4j extends sys.std {
   compiledOnly = true;

   // Log4j complains if we include two incompatible implementation classes and so higher level 
   // packages need to define the impl library.  Here we only pull in the api.

   object slf4jApiPkg extends MvnRepositoryPackage {
      url = "mvn://org.slf4j/slf4j-api/1.7.0";
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
         addPatterns("log4j\\.properties");
      }
   }

   // This method is run when the layer is initialized.  
   public void init() {
      // log4j is automatically excluded from these runtimes
      excludeRuntimes("js", "android", "gwt");
   }
}

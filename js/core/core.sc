package sc.js;

public js.core extends html.core, js.prebuild, js.sys, js.util, sys.sccore {
   compiledOnly = true;

   codeType = sc.layer.CodeType.Framework;

   public void init() {
      excludeRuntimes("java", "gwt", "android");
   }

   webFileProcessor {
      templatePrefix = defaultWebRoot;
   }
  
   public void start() {
      sc.layer.LayeredSystem system = getLayeredSystem();

      if (system.runtimeProcessor instanceof sc.lang.js.JSRuntimeProcessor)
          ((sc.lang.js.JSRuntimeProcessor) system.runtimeProcessor).destinationName = "jsHttp";

      sc.lang.TemplateLanguage tempLang = (sc.lang.TemplateLanguage) system.getFileProcessorForExtension("schtml");
      tempLang.processPrefix = defaultWebRoot;

      // Files in the web directory are marked as type 'web' and go into the defaultWebRoot 'web'
      addSrcPath("web", "web", defaultWebRoot);

      // So JS code can use these scopes which are defined in the servlet.core layer.  But they all map to global on the client
      registerScopeAlias("session", "global");
      registerScopeAlias("appSession", "global");
      registerScopeAlias("appGlobal", "global");
      registerScopeAlias("window", "global");

      // Adds a static code snippet to register the page when you annotate a class with @URL.
      // If we happen to register an inner class the addPage still goes on the parent type
      sc.lang.DefaultAnnotationProcessor urlProc = new sc.lang.DefaultAnnotationProcessor();
      urlProc.staticMixinTemplate = "sc.js.URLMixinTemplate";
      urlProc.validOnField = false;
      urlProc.validOnClass = true;
      urlProc.validOnObject = true;
      urlProc.initOnStartup = true;
      urlProc.typeGroupName = "URLTypes";
      urlProc.inherited = true; // Include any sub-type which has URL in the type group
      urlProc.skipAbstract = true; // Don't include any abstract classes or templates with abstract="true"
      registerAnnotationProcessor("sc.html.URL", urlProc);

      // Only want sh files generated once - using the server's runtime
      if (system.serverEnabled) {
         sc.lang.TemplateLanguage scshLang = (sc.lang.TemplateLanguage) system.getFileProcessorForExtension("scsh");
         scshLang.disableProcessing = true;
      }
   }
}

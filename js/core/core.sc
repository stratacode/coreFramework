package sc.js;

public js.core extends html.core, js.prebuild, js.sys, js.util, sys.sccore {
   compiledOnly = true;

   codeType = sc.layer.CodeType.Framework;
   codeFunction = sc.layer.CodeFunction.Program;

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
   }
}

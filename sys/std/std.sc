sys.std {
   codeType = sc.layer.CodeType.Framework;
   codeFunction = sc.layer.CodeFunction.Program;


   hidden = true;
   compiledOnly = true;

   object configFileProcessor extends LayerFileProcessor {
      prependLayerPackage = false;
      useSrcDir = false;
      extensions = {"xml", "properties", "css", "sh", "policy", "xsd"};
      srcPathTypes = {null, "config"};
   }


   object resourceFileProcessor extends LayerFileProcessor {
      prependLayerPackage = false;
      useSrcDir = false;
      useClassesDir = true;
      extensions = {"xml", "properties", "css", "sql"};
      srcPathTypes = {"resource"};
   }

   public void start() {
      LayeredSystem system = getLayeredSystem();
/*
      LayerFileProcessor configFileProcessor = new LayerFileProcessor();

      configFileProcessor.prependLayerPackage = false;
      configFileProcessor.useSrcDir = false;

      // Copy these extensions to the output file
      registerFileProcessor(configFileProcessor, "xml");
      registerFileProcessor(configFileProcessor, "properties");
      registerFileProcessor(configFileProcessor, "css");

      // Need to add extensions of any files produced so that we know where they are in the build src index - src dir (e.g. java or js), output dir - e.g. build or build/web).
      registerFileProcessor(configFileProcessor, "sh");
*/

      sc.lang.TemplateLanguage templateResourceLang = new sc.lang.TemplateLanguage();
      templateResourceLang.processTemplate = true;
      templateResourceLang.buildPhase = sc.layer.BuildPhase.Process;
      templateResourceLang.resultSuffix = "xml";
      templateResourceLang.useSrcDir = false;
      templateResourceLang.prependLayerPackage = false;
      templateResourceLang.srcPathTypes = new String[] {null, "web", "resource", "config"};

      registerLanguage(templateResourceLang, "scxml");

      sc.lang.TemplateLanguage scshLang = new sc.lang.TemplateLanguage();
      scshLang.processTemplate = true;
      scshLang.buildPhase = sc.layer.BuildPhase.Process;
      scshLang.resultSuffix = "sh";
      scshLang.useSrcDir = false;
      scshLang.prependLayerPackage = false;
      scshLang.makeExecutable = true;
      scshLang.needsJavascript = false;

      registerLanguage(scshLang, "scsh");
   }
}

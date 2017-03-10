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

      // Registers a standard scope which is "per-application", where the application is defined by the ScopeEnvironment's appId value.
      // For a web application, the appId defaults to the base part of the URL.  For a desktop application 'null' is the default app which
      // will work just like global but it's a hook to allow multiple independent resident apps to use independent instances of the same
      // components or maybe support a multi-tenant application.
      sc.lang.sc.BasicScopeProcessor appGlobalScope = new sc.lang.sc.BasicScopeProcessor("appGlobal");
      appGlobalScope.validOnClass = true;
      appGlobalScope.validOnField = false;
      appGlobalScope.validOnObject = true;
      appGlobalScope.includeScopeAnnotation = true;
      appGlobalScope.needsField = false;
      appGlobalScope.customResolver = 
          "      sc.obj.ScopeContext _ctx = sc.obj.AppGlobalScopeDefinition.getAppGlobalScope();\n" +
          "      if (_ctx == null) return null;\n" +
          "      <%= variableTypeName %> _<%= lowerClassName %> = (<%= variableTypeName %>) _ctx.getValue(\"<%= typeClassName %>\");\n";
      appGlobalScope.customSetter = 
          "      _ctx.setValue(\"<%= typeClassName %>\", _<%= lowerClassName %>);\n";
      registerScopeProcessor("appGlobal", appGlobalScope);

      // Defines a new object lifecycle where instances are stored for each request
      sc.lang.sc.BasicScopeProcessor requestScope = new sc.lang.sc.BasicScopeProcessor("request");
      requestScope.validOnClass = true;
      requestScope.validOnField = false;
      requestScope.validOnObject = true;
      requestScope.includeScopeAnnotation = true;
      requestScope.needsField = false;
      requestScope.customResolver = 
          "      <%= variableTypeName %> _<%= lowerClassName %> = (<%= variableTypeName %>) sc.obj.RequestScopeDefinition.getValue(\"<%= typeClassName %>\");\n";
      requestScope.customSetter = 
          "      sc.obj.RequestScopeDefinition.setValue(\"<%= typeClassName %>\", _<%= lowerClassName %>);\n";
      // Changes how synchronization is performed in this scope.  It will use a stateless protocol - transferring all synchronized state on each request. 
      // Automatic sync is disabled
      requestScope.temporaryScope = true; 
      registerScopeProcessor("request", requestScope);
   }
}

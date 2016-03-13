package sc.js;

js.prebuild {
   buildLayer = true;
   finalLayer = true;
   compiledOnly = true;

   codeType = sc.layer.CodeType.Framework;
   codeFunction = sc.layer.CodeFunction.Program;

   public void init() {
      includeRuntime("js");
   }

   public void start() {
      sc.layer.LayeredSystem system = getLayeredSystem();

      // JS type templates can be compiled using this definition.
      sc.lang.TemplateLanguage jsTypeTemplateLang = new sc.lang.TemplateLanguage();

      jsTypeTemplateLang.compiledTemplate = true;

      jsTypeTemplateLang.prependLayerPackage = true;
      // As a type we need the package but for saving the result file we do not (when compiledTemplate=true and processTemplate=true)
      jsTypeTemplateLang.prependLayerPackageOnProcess = false;

      jsTypeTemplateLang.definedInLayer = this;
      jsTypeTemplateLang.buildPhase = sc.layer.BuildPhase.Process;
      jsTypeTemplateLang.useSrcDir = false;
      jsTypeTemplateLang.needsOutputMethod = true;
      jsTypeTemplateLang.needsJavascript = false;

      jsTypeTemplateLang.defaultExtendsType = "sc.lang.js.JSTypeTemplateBase";
      registerLanguage(jsTypeTemplateLang, "sctjs");
   }
}

package sc.js;

/** This framework layer defines the default JSTypeTemplate, used for converting a Java type to Javascript */
js.prebuild {
   /*
    * Make this a build layer so we pre-compile the template rather than interpreting it. Because it has no dependencies, it's built before
    * the other layers and because it's final the compiled classes are available to the runtime so we pick up the compiled version of the template
    */
   buildLayer = true;
   finalLayer = true;
   compiledOnly = true;

   codeType = sc.layer.CodeType.Framework;

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

      jsTypeTemplateLang.templateModifiers = new sc.lang.SemanticNodeList();
      jsTypeTemplateLang.templateModifiers.add("public");

      jsTypeTemplateLang.defaultExtendsType = "sc.lang.js.JSTypeTemplateBase";
      registerLanguage(jsTypeTemplateLang, "sctjs");
   }
}

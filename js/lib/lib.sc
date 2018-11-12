package sc.js;

//import sc.js.MainInit;

/** The layer which defines the Javascript runtime.  Normally extended only by other framework layers. */
js.lib {
   static String GEN_JS_PREFIX = "js/types";

   codeType = CodeType.Framework;

   compiledOnly = true;
   finalLayer = true;

   public void init() {
      // Defines the javascript runtime.  This creates a new LayeredSystem for Javascript and makes this the foundational layer 
      // of that LayeredSystem.
      LayeredSystem sys = getLayeredSystem();
      sc.lang.js.JSRuntimeProcessor runtimeProc = (sc.lang.js.JSRuntimeProcessor)
                                   LayeredSystem.getRuntime("js");
      if (runtimeProc == null) {
         runtimeProc = new sc.lang.js.JSRuntimeProcessor();
         runtimeProc.addSyncProcessName("Server");
         addRuntime(runtimeProc);
      }

      //runtimeProc.templatePrefix = "web";
      runtimeProc.srcPathType = "web";
      runtimeProc.genJSPrefix = GEN_JS_PREFIX;  // Prefix to store the generated per-type JS files
      // Name for a SC template which uses sc.lang.js.JSTypeParameters to generate javascript code from stratacode in these contexts:
      //  - java type to js
      runtimeProc.typeTemplateName = "sc.js.JSTypeTemplate";
      //  - sync update
      runtimeProc.syncMergeTemplateName = "sc.js.JSSyncMergeTemplate";
      //  - js code change update
      runtimeProc.updateMergeTemplateName = "sc.js.JSUpdateMergeTemplate";
      //  - code to evaluate a sync 'eval' expression - resolve context object and run JS code snippet
      runtimeProc.evalTemplateName = "sc.js.JSEvalTemplate";

      // Binds this layer to this runtime.  It will be excluded from other runtimes even if they have dependencies on this layer.
      setLayerRuntime(runtimeProc);
   }

   public void start() {
      LayeredSystem system = getLayeredSystem();

      // For Javascript, property mappers are probably not worth it - use just use native hashtable string keys for properties like everyone else
      // Need to wait to do this till start because init is called even for the main runtime and we don't want to change the mode for that one
      if (activated)
         system.usePropertyMappers = false;

      /*
      sc.lang.DefaultAnnotationProcessor mainInitProc = new sc.lang.DefaultAnnotationProcessor();
      mainInitProc.typeGroupName = "mainInit";
      mainInitProc.validOnField = false;
      mainInitProc.validOnObject = true;
      mainInitProc.inherited = true; // Include any sub-type which has MainInit
      mainInitProc.skipAbstract = true; // Don't include any abstract macro templates
      mainInitProc.subTypesOnly = true; // Don't include the Html and Page types which set the annotation
      registerAnnotationProcessor("sc.js.MainInit", mainInitProc);
      */

      // Register a file processor that recognizes the generated js src files for each type.
      LayerFileProcessor jsFileProcessor = new LayerFileProcessor();
      jsFileProcessor.prependLayerPackage = false;
      jsFileProcessor.useSrcDir = true;
      system.registerPatternFileProcessor(GEN_JS_PREFIX + "/.*", jsFileProcessor, this);
   }
}

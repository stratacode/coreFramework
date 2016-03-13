// Toggle's the systems use of jsModuleFiles, except on entry points marked with @MainInit.  
// Each MainInit method will then generate a file with all of its dependencies, instead of shared libraries.
js.options.disableModules extends js.core {
   void start() {
      if (activated) {
         sc.lang.js.JSRuntimeProcessor proc = (sc.lang.js.JSRuntimeProcessor) layeredSystem.getRuntime("js");
         proc.disableModules = true;
      }
   }
}

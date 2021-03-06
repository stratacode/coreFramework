// This layer places the StrataCode runtime source files for the 'core' section so they can be compiled
// into Javascript.  Currently it does not allow you to modify those types - i.e. to customize the corert package itself.
// It should only be included into Javascript just for performance because we might inadvertently load some source files
// from this layer when looking up the compiled types.
sys.sccore {
   codeType = sc.layer.CodeType.Framework;

   hidden = true; // don't show it in the IDE
   compiledOnly = true; // don't try to interpret this layer
   finalLayer = true;  // No modifying these classes or anything they depend on

   compiled = true; // This layer stores src files that are already compiled - by marking this as both final and compiled, we do not have to load the source unless re-generating these classes in javascript.

   public void init() {
      sc.layer.LayeredSystem system = getLayeredSystem();

      if (activated) { // if we are in the IDE (not activated), it's better to find the source files in the source modules configured in intelliJ
         // Pick up the src files for this layer from the system's corert directory
         String rtSrcDir = system.getStrataCodeRuntimePath(RuntimeModuleType.CoreRuntime, true);
         preCompiledSrcPath = rtSrcDir;

         if (rtSrcDir == null)
            system.error("No stratacode source for layer: " + this + " runtime: " + system.getProcessIdent());
         else if (system.options.verbose)
            system.verbose("Core runtime src: " + rtSrcDir + " from layer: " + this + " runtime: " + system.getProcessIdent());
      }

      excludeRuntime("java");
   }


}

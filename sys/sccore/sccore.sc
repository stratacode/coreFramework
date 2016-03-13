// This layer places the StrataCode runtime source files for the 'core' section so they can be compiled
// into Javascript.  Currently it does not allow you to modify those types - i.e. to customize the corert package itself.
// It should only be included into Javascript just for performance because we might inadvertently load some source files
// from this layer when looking up the compiled types.
sys.sccore {
   codeType = sc.layer.CodeType.Framework;
   codeFunction = sc.layer.CodeFunction.Program;

   hidden = true; // don't show it in the IDE
   compiledOnly = true; // don't try to interpret this layer
   finalLayer = true;  // No modifying these classes or anything they depend on

   compiled = true; // This layer stores src files that are already compiled - by marking this as both final and compiled, we do not have to load the source unless re-generating these classes in javascript.

   public void init() {
      sc.layer.LayeredSystem system = getLayeredSystem();

      // Pick up the src files for this layer from the system's corert directory
      String rtSrcDir = system.getStrataCodeRuntimePath(true, true);
      preCompiledSrcPath = rtSrcDir;

      excludeRuntime("java");
   }


}

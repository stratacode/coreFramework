// TODO: REMOVE THIS - NO LONGER USED
// Provides functionality for applications that want to use TypeDeclarations as objects, i.e. to edit layer cake applications on the fly.
sys.layerEdit {
   codeType = sc.layer.CodeType.Framework;
   codeFunction = sc.layer.CodeFunction.Program;

   hidden = true;
   compiledOnly = true;

/*
   public void init() {
      if (layeredSystem.getLayerByDirName("js.core") != null) {
         layeredSystem.insertLayer("js.layer", getLayerPosition());
      }
   }
*/

   public void start() {
      // TODO: remove this whole typesAreObjects thing - it's a memory now with ClientTypeDeclaration
      // the code unfortunately treats TypeDeclarations as special class objects in dynamic mode.  So when we are running the editor we can serialize these types as objects.  
      // The sync handler -or maybe the editor itself? - should wrap them in objects which can be serialized.
      /*
      boolean verbose = layeredSystem.options.verbose;
      if (!layeredSystem.hasDynamicLayers()) {
         //layeredSystem.typesAreObjects = true;
         //if (verbose)
            //System.out.println("Enabling typesAreObjects mode");
      }
      else if (layeredSystem.runtimes != null && layeredSystem.runtimes.size() > 1)
         System.err.println("*** Dynamic layers used with multiple runtimes and the sys.layerEdit layer.  Types can't be serialized in this configuration");
      else if (verbose)
         System.out.println("Not enabling typesAreObjects mode");
      */
   }
}

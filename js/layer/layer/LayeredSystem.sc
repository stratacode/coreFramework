import sc.obj.Constant;
import sc.obj.Sync;
import sc.js.URLPath;
import sc.bind.Bind;

import java.util.List;

/** The client view of this file (a subset of the original one -
 * TODO: should be using layers to keep them in sync but that will require breaking the cord with Java and building SC with SC
 */
@Sync(onDemand=true)
public class LayeredSystem {
   public List<Layer> layers;

   public static LayeredSystem current = null;

   public LayeredSystem() {
      current = this;
   }

   @Constant
   public Options options = new Options();

   @Sync(onDemand=true)
   public static class Options {
      public boolean buildAllFiles;            // Re-generate all source files when true.  The default is to use dependencies to only generate changed files.
      public boolean buildAllLayers;           // When true, do not inherit files from previous layers.  The buildDir will have all java files, even from layers that are already compiled
      public boolean noCompile;
      @Constant
      public boolean verbose = false;          // Controls debug level verbose messages
      public boolean info = true;
      public boolean debug = true;             // Controls whether java files compiled by this system debuggable
      public boolean crossCompile = false;
      public boolean runFromBuildDir = false;  // Change to the buildDir before running the command
      public boolean runScript = false;
      public boolean createNewLayer = false;
      public boolean dynamicLayers = false;
      public boolean allDynamic = false;       // -dynall: like -dyn but all layers included by the specified layers are also made dynamic
      /** When true, we maintain the reverse mapping from type to object so that when certain type changes are made, we can propagate those changes to all instances */
      public boolean liveDynamicTypes = true;
      /** When you have multiple build layers, causes each subsequent layer to get all source/class files from the previous. */
      public boolean useCommonBuildDir = false;
      public String buildDir;
      public String buildSrcDir;
      public String recordFile; // File used to record script by default
      public String restartArgsFile;
      public boolean compileOnly = false;  // Enabled with the -c option - only compile, do not run either main methods or runCommands.
   }

   public Layer getLayerByDirName(String dirName) {
      if (layers == null)
         return null;

      for (Layer l:layers)
         if (l.layerUniqueName.equals(dirName))
            return l;

      return null;
   }

   private boolean staleCompiledModel;
   public boolean getStaleCompiledModel() {
      return staleCompiledModel;
   }
   public void setStaleCompiledModel(boolean v) {
      staleCompiledModel = v;
      Bind.sendChangedEvent(this, "staleCompiledModel");
   }

   public static LayeredSystem getCurrent() {
      return null;
   }

   public static List<URLPath> getURLPaths() {
      return null;
   }

   public boolean serverEnabled;

   public boolean testPatternMatches(String pattern) {
      return true;
   }

   public String getServerURL() {
      return null;
   }

}

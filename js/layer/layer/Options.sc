import sc.obj.Constant;
import sc.obj.Sync;

@Sync(onDemand=true)
public class Options {
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
   @Constant
   public boolean liveDynamicTypes = true;
   /** When you have multiple build layers, causes each subsequent layer to get all source/class files from the previous. */
   @Constant
   public boolean useCommonBuildDir = false;
   @Constant
   public String buildDir;
   @Constant
   public String buildSrcDir;
   @Constant
   public String recordFile; // File used to record script by default
   @Constant
   public String restartArgsFile;
   @Constant
   public boolean compileOnly = false;  // Enabled with the -c option - only compile, do not run either main methods or runCommands.
   @Constant
   public String testScriptName = null; 
   @Constant
   public String testResultsDir = null;  
   @Constant
   public boolean headless = false;  
   @Constant
   public boolean testVerifyMode = false;
   @Constant
   public boolean testDebugMode = false;
}

import sc.obj.Constant;
import sc.obj.Sync;
import sc.js.URLPath;
import sc.bind.Bind;
import sc.lang.java.BodyTypeDeclaration;

import sc.dyn.DynUtil;

import java.util.List;
import java.util.HashMap;

import sc.type.IResponseListener;

/** The client view of the LayeredSystem which is a subset of the real LayeredSystem
 * TODO: should be using layers to keep this class in sync with the original but that will require building SC with SC which has some downsides
 */
//@Sync(onDemand=true)
public class LayeredSystem {
   public List<Layer> layers;

   public HashMap<String,BodyTypeDeclaration> typesByNameIndex = new HashMap<String,BodyTypeDeclaration>();

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
      return current;
   }

   public static List<URLPath> getURLPaths() {
      return null;
   }

   public boolean serverEnabled;

   public boolean getServerEnabled() {
      return serverEnabled;
   }

   public boolean syncEnabled;

   public static String getURLForPath(String name) {
      return null;
   }

   public boolean testPatternMatches(String pattern) {
      return true;
   }

   public String getServerURL() {
      return null;
   }

   private class FetchTypeResponseListener implements IResponseListener {
      String typeName;
      IResponseListener wrapped;
      FetchTypeResponseListener(String typeName, IResponseListener wrap) {
         this.wrapped = wrap;
         this.typeName = typeName;
      }

      public void response(Object response) {
         typesByNameIndex.put(typeName, (BodyTypeDeclaration)response);
         if (response != null)
            wrapped.response(response);
      }
      public void error(int errorCode, Object error) {
         System.err.println("*** Error trying to fetch type declaration: " + errorCode + ": " + error);
      }
   }

   public void fetchRemoteTypeDeclaration(String typeName, IResponseListener resp) {
      // We cache null if there's no src type declaration to avoid trying this over and over again
      if (!typesByNameIndex.containsKey(typeName)) {
         sc.dyn.RemoteResult res = DynUtil.invokeRemote(null, null, null, this, DynUtil.resolveRemoteMethod(this, "getSrcTypeDeclaration", Object.class, "Ljava/lang/String;"), typeName);
         res.responseListener = new FetchTypeResponseListener(typeName, resp);
      }
   }

   public boolean waitForRuntime(long timeout) {
      return true;
   }

   public LayeredSystem getPeerLayeredSystem(String processIdent) {
      return null;
   }

   public void addSystemExitListener(sc.obj.ISystemExitListener l) {}
}

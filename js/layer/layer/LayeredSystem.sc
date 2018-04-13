import sc.obj.Constant;
import sc.obj.Sync;
import sc.js.URLPath;
import sc.bind.Bind;
import sc.lang.java.BodyTypeDeclaration;

import sc.dyn.DynUtil;

import java.util.List;
import java.util.HashMap;

import sc.type.IResponseListener;

/** The client view of the LayeredSystem which is a subset of the real LayeredSystem.  Used to store the synchronized info from the server, and for the client
 * to update server options, and get client-views of the layer and type metadata from the server.
 * TODO: we should be using layers to keep this class in sync with the original but that will require building SC with SC which has some downsides
 */
//@Sync(onDemand=true) - this is what the server sets via the API in code
public class LayeredSystem {
   public List<Layer> layers;

   public HashMap<String,BodyTypeDeclaration> typesByNameIndex = new HashMap<String,BodyTypeDeclaration>();

   public static LayeredSystem current = null;

   public LayeredSystem() {
      current = this;
   }

   @Constant
   public Options options = new Options();

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

   public BuildInfo buildInfo;

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

import sc.obj.Constant;
import sc.obj.Sync;
import sc.js.URLPath;
import sc.bind.Bind;
import sc.lang.java.BodyTypeDeclaration;
import sc.lang.java.TypeDeclaration;

import sc.dyn.DynUtil;

import java.util.List;
import java.util.HashMap;
import java.util.TreeMap;

import sc.type.IResponseListener;

/** The client view of the LayeredSystem which is a subset of the real LayeredSystem.  Used to store the synchronized info from the server, and for the client
 * to update server options, and get client-views of the layer and type metadata from the server.
 * TODO: we should be using layers to keep this class in sync with the original but that will require building SC with SC which has some downsides
 */
//@Sync(onDemand=true) - this is what the server sets via the API in code
public class LayeredSystem {
   public List<Layer> layers;

   HashMap<String,BodyTypeDeclaration> typesByNameIndex = new HashMap<String,BodyTypeDeclaration>();
   TreeMap<String,FetchTypeResponseListener> beingFetched = new TreeMap<String,FetchTypeResponseListener>();

   public BodyTypeDeclaration getSrcTypeDeclaration(String typeName, Layer refLayer) {
      return typesByNameIndex.get(typeName);
   }
   
   public void addTypeDeclaration(String typeName, BodyTypeDeclaration toAdd) {
      typesByNameIndex.put(typeName, toAdd);
   }

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

   public Layer getLayerByName(String layerName) {
      if (layers == null)
         return null;

      for (Layer l:layers)
         if (l.layerName.equals(layerName))
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
      List<IResponseListener> chainedListeners;
      FetchTypeResponseListener(String typeName, IResponseListener wrap) {
         this.wrapped = wrap;
         this.typeName = typeName;
      }

      public void response(Object response) {
         beingFetched.remove(typeName);
         typesByNameIndex.put(typeName, (BodyTypeDeclaration)response);
         if (response != null) {
            wrapped.response(response);
            if (chainedListeners != null) {
               for (IResponseListener cl:chainedListeners)
                  cl.response(response);
            }
         }
      }
      public void error(int errorCode, Object error) {
         beingFetched.remove(typeName);
         System.err.println("*** Error trying to fetch type declaration: " + errorCode + ": " + error);
         wrapped.error(errorCode, error);
      }

      public void addChainedListener(IResponseListener resp) {
         if (chainedListeners == null)
            chainedListeners = new java.util.ArrayList<IResponseListener>();
         chainedListeners.add(resp);
      }
   }

   public void fetchRemoteTypeDeclaration(String typeName, IResponseListener resp) {
      if (resp == null)
         System.out.println("***");
      // We cache null if there's no src type declaration to avoid trying this over and over again
      if (!typesByNameIndex.containsKey(typeName)) {
         FetchTypeResponseListener listener = beingFetched.get(typeName);
         if (listener != null) {
            listener.addChainedListener(resp);
            return;
         }
         sc.dyn.RemoteResult res = DynUtil.invokeRemote(null, null, null, this, DynUtil.resolveRemoteMethod(this, "getSrcTypeDeclaration", Object.class, "Ljava/lang/String;"), typeName);
         FetchTypeResponseListener ftrl = new FetchTypeResponseListener(typeName, resp);
         // In the response listener, we might not have set all of the properties of the returned type, so it's more convenient to notify these after the
         // sync has completed.
         //res.responseListener = ftrl;
         res.postListener = ftrl;
         beingFetched.put(typeName, ftrl);
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

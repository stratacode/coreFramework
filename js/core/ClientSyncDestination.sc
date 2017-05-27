import sc.type.PTypeUtil;
import sc.type.CTypeUtil;

import sc.type.IResponseListener;
import sc.sync.SyncManager;
import sc.sync.SyncDestination;
import sc.sync.SyncPropOptions;

import sc.bind.BindingContext;

import java.util.Arrays;

import sc.dyn.DynUtil;

import sc.lang.html.Window;

import sc.obj.GlobalScopeDefinition;
import sc.obj.ScopeDefinition;

@Component
@MainInit
@JSSettings(jsModuleFile="js/sync.js", prefixAlias="sc_", requiredModule=true)
object ClientSyncDestination extends SyncDestination {
   name = "servletToJS";
   defaultScope = "global";

   allowCodeEval = true; // Allows the browser to evaluate code that's sent from the server
   clientDestination = true; // Objects received by this destination are registered as 'fixed' - i.e. we don't post them back on a reset

   public void writeToDestination(String layerDef, String syncGroup, IResponseListener listener, String paramStr, CharSequence codeUpdates) {
      String useParams = paramStr;
      if (syncGroup != null) {
         String syncParam = "syncGroup=" + syncGroup;
         useParams = useParams == null ? syncParam : useParams + "&" + syncParam;
      }
      String urlParam = null;
      Window w = Window.getWindow();
      if (w != null)
         urlParam = "url=" + CTypeUtil.escapeURLString(w.location.pathname);
      if (useParams == null)
         useParams = urlParam == null ? "" : "?" + urlParam;
      else
         useParams = "?" + useParams + (urlParam == null ? "" : "&" + urlParam);

      int winId = PTypeUtil.getWindowId();
      if (winId != -1) {
         if (useParams == null)
            useParams = "?";
         else
            useParams += "&";
         useParams += "windowId=" + winId;
      }
      if (useParams == null)
         useParams = "?";
      else
         useParams += "&";
      useParams += "lang=" + sendLanguage;
      PTypeUtil.postHttpRequest("/sync" + useParams, layerDef, "text/plain", listener);
   }

   void init() {
      ScopeDefinition.initScopes();
      // On the client global and session are the same thing - i.e. one instance per user's session
      GlobalScopeDefinition.getGlobalScopeDefinition().aliases.addAll(Arrays.asList(new String[]{"session","window"}));
      SyncManager.addSyncDestination(this);
   }

   // The server returns a javascript encoded result to the layer sync operation.  Just apply these changes by evaluating them in the JS runtime.
   public void applySyncLayer(String toApply, String receiveLanguage, boolean isReset) {
      if (receiveLanguage == null && toApply != null && !toApply.startsWith("sync:"))
         receiveLanguage = "js";
      if (SyncManager.trace) {
         if (toApply == null || toApply.length() == 0)
            System.out.println("Server returned no changes");
         else
            System.out.println("Applying changes" + (receiveLanguage == null ? "" : " in " + receiveLanguage) + " from server: '" + toApply + "'" + (isReset ? "reset" : "") +"\n");
      }
      if (receiveLanguage != null && receiveLanguage.equals("js")) {
         DynUtil.evalScript(toApply);
         if (SyncManager.trace) {
            System.out.println("Eval complete");
         }
      }
      else {
         super.applySyncLayer(toApply, receiveLanguage, isReset);
      }
   }

   public void initSyncManager() {
      syncManager = new ClientSyncManager(this);
   }

   /** Sending the raw layer definition to the server as it can parse it easily there. */
   public StringBuilder translateSyncLayer(String layerDef) {
      StringBuilder sb = new StringBuilder();
      sb.append(layerDef);
      return sb;
   }

   public int getDefaultSyncPropOptions() {
      return SyncPropOptions.SYNC_CLIENT;
   }

   public boolean isSendingSync() {
      return true;
   }
}

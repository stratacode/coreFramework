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

@Component
@MainInit
@JSSettings(jsModuleFile="js/sync.js", prefixAlias="sc_", requiredModule=true)
object ClientSyncDestination extends SyncDestination {
   name = "servletToJS";

   public void writeToDestination(String layerDef, String syncGroup, IResponseListener listener, String paramStr) {
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
      PTypeUtil.postHttpRequest("/sync" + useParams, layerDef, "text/plain", listener);
   }

   void init() {
      // On the client global and session are the same thing - i.e. one instance per user's session
      GlobalScopeDefinition.getGlobalScopeDefinition().aliases = Arrays.asList(new String[]{"session","window"});
      SyncManager.addSyncDestination(this);
   }

   // The server returns a javascript encoded result to the layer sync operation.  Just apply these changes by evaluating them in the JS runtime.
   public void applySyncLayer(String toApply, boolean isReset) {
      BindingContext ctx = new BindingContext();
      BindingContext oldBindCtx = BindingContext.getBindingContext();
      BindingContext.setBindingContext(ctx);
      try {
         if (SyncManager.trace) {
            if (toApply == null || toApply.length() == 0)
               System.out.println("Server returned no changes");
            else
               System.out.println("Evaluating JS script returned from server: '" + toApply + "'\n");
         }
         DynUtil.evalScript(toApply);
         if (SyncManager.trace) {
            System.out.println("Eval complete");
         }
      }
      finally {
         BindingContext.setBindingContext(oldBindCtx);
         // By batching the events we are giving the code a simpler model to deal with in the event callbacks - i.e. objects are fully populated when property changes are fired.
         ctx.dispatchEvents(null);
      }
   }

   public void initSyncManager() {
      syncManager = new ClientSyncManager(this);
   }

   /** Sending the raw layer definition to the server as it can parse it easily there. */
   public CharSequence translateSyncLayer(String layerDef) {
      return layerDef;
   }

   public int getDefaultSyncPropOptions() {
      return SyncPropOptions.SYNC_CLIENT;
   }

   public boolean isSendingSync() {
      return true;
   }
}

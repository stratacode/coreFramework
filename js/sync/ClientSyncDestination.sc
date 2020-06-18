import sc.type.PTypeUtil;
import sc.type.CTypeUtil;

import sc.type.IResponseListener;
import sc.sync.SyncManager;
import sc.sync.SyncDestination;
import sc.sync.SyncPropOptions;

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

   {
      // When loaded as a local file on the client, turn off real time since there's no server to talk to
      String serverName = PTypeUtil.getServerName();
      if (serverName == null || serverName.equals(""))
         realTime = false;
   }

   /** How much time should we wait on the server for changes?  This can be set as high as it's ok to keep an HTTP connection open  */
   int waitTime = realTime ? 1200000 : -1;

   /**
    * Incremented each time we send a sync. Used by the client to determine when response data is stale based on a subsequent change - e.g. changing a value
    * field of an input tag twice before processing the response from the first. If the server decides to reformat value in the meantime, we need to ignore that
    * change or it will replace the more recent change from the user.
    */
   //long sequenceNumber = 0;

   public void writeToDestination(String layerDef, String syncGroup, IResponseListener listener, String paramStr, CharSequence codeUpdates) {
      String useParams = getIdParams(paramStr, syncGroup);

      if (useParams == null)
         useParams = "?";
      else
         useParams += "&";
      useParams += "lang=" + sendLanguage;
      // Never wait when we are posting data - aka a 'send' versus a 'sync'
      // Also don't wait until we know we are connected - so we'll do an extra sync at startup just to be sure we really are
      // connected to the server.
      if (waitTime != -1 && layerDef.length() == 0 && connected)
         useParams += "&waitTime=" + waitTime;
      if (SyncManager.trace) {
         System.out.println("Sync: POST /sync" + useParams);
      }
      PTypeUtil.postHttpRequest("/sync" + useParams, layerDef, "text/plain", listener);
   }

   // Returns the URL parameters needed to identify our session - shared by sync and sync-close
   String getIdParams(String useParams, String syncGroup) {
      if (syncGroup != null) {
         String syncParam = "syncGroup=" + syncGroup;
         useParams = useParams == null ? syncParam : useParams + "&" + syncParam;
      }
      String urlParam = null;
      Window w = Window.getWindow();
      if (w != null)
         urlParam = "url=" + CTypeUtil.escapeURLString(w.location.pathname);
      if (useParams == null)
         useParams = urlParam == null ? null : "?" + urlParam;
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

      return useParams;
   }

   void init() {
      ScopeDefinition.initScopes();
      // On the client global, session etc are the same thing - i.e. one instance per user's session
      GlobalScopeDefinition.getGlobalScopeDefinition().aliases.addAll(Arrays.asList(new String[]{"session","window","appSession","appGlobal","request"}));
      SyncManager.addSyncDestination(this);

      DynUtil.addSystemExitListener(new sc.obj.ISystemExitListener() {
         void systemExiting() {
            PTypeUtil.sendBeacon("/sync" + getIdParams("close=true", null), "");
         }
      });
   }

   // Apply changes received from the server, either by evaluating JS or use the super method which uses the deserializer to apply the sync response
   public boolean applySyncLayer(String toApply, String receiveLanguage, ScopeDefinition syncScope, boolean applyingRemoteReset, String detail) {
      if (receiveLanguage == null && toApply != null && !toApply.startsWith("sync:"))
         receiveLanguage = "js";
      if (SyncManager.trace) {
         if (toApply == null || toApply.length() == 0)
            System.out.println("No changes in server response");
         else
            System.out.println("Applying server " + detail + ": " + (receiveLanguage == null ? "changes" : receiveLanguage) + ": '" + toApply + "'" + (applyingRemoteReset ? " applying remote reset" : "") +"\n");
      }
      if (receiveLanguage != null && receiveLanguage.equals("js")) {
         DynUtil.evalScript(toApply);
         if (SyncManager.trace) {
            System.out.println("Eval complete");
         }
         if (toApply != null && toApply.length() > 0) {
            SyncManager.callAfterApplySync();
            return true;
         }
         return false;
      }
      else {
         return super.applySyncLayer(toApply, receiveLanguage, syncScope, applyingRemoteReset, detail);
      }
   }

   public void initSyncManager() {
      syncManager = new ClientSyncManager(this);

      // Hook into the synchronization system so we can resolve sync updates for DOM elements - by id for which
      // there is no tag class on the client - the server tags.  A resolver to lookup or create the tag object for
      // this lookup in the sync system if there is one.
      syncManager.addFrameworkNameContext(new sc.dyn.INameContext() {
         public Object resolveName(String name, boolean create, boolean returnTypes) {
            return Element.updateServerTag(null, name, null, false);
         }
      });

      syncManager.addFrameworkListener(new sc.sync.IFrameworkListener() {
         public void afterApplySync() {
            Element.scheduleRefresh();
         }
      });
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

   public boolean isClient() {
      return true;
   }

   /** After we've received the response from one sync, unless we've already scheduled another, set up a job to resync if we are doing realtime */
   public void postCompleteSync() {
      if (pollTime != -1 && numSendsInProgress == 0 && numWaitsInProgress == 0 && connected) {
         PTypeUtil.addScheduledJob(new Runnable() {
            public void run() {
               if (numSendsInProgress == 0 && numWaitsInProgress == 0) {
                  syncManager.autoSync();
               }
            }
         }, pollTime, false);
      }
   }
}

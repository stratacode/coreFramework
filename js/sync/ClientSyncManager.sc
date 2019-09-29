import sc.sync.SyncManager;
import sc.sync.SyncDestination;
import sc.type.PTypeUtil;
import sc.obj.GlobalScopeDefinition;
import sc.obj.AppGlobalScopeDefinition;
import sc.obj.RequestScopeDefinition;

@sc.js.JSSettings(jsModuleFile="js/sync.js", prefixAlias="sc_", requiredModule=true)
class ClientSyncManager extends SyncManager {
   boolean autoSync = true;      // If true, should be synchronize as soon as stale changes are noticed
   String autoSyncGroup = null;  // Sync group to use when auto-syncing
   String autoSyncDest = null;   // Optional destination to use for auto sync.  If not specified, all destinations are auto-synced.

   int syncMinDelay = 100;       // Specifies a minimum time between syncs, so we do not send too many syncs back to back (in milliseconds)

   long lastSentTime = -1;

   recordInitial = false;        // By default, let the server restore the initial state and only record the changes made from the initial state.  This is used to restore the server session when it gets lost.0

   /** Set at code init time to true if there's no session for this sync */
   static boolean statelessServer = false;
   static boolean defaultRealTime = true;

   ClientSyncManager(SyncDestination dest) {
      super(dest);

      GlobalScopeDefinition.getGlobalScopeDefinition().supportsChangeEvents = true;
      AppGlobalScopeDefinition.getAppGlobalScopeDefinition().supportsChangeEvents = true;
      RequestScopeDefinition.getRequestScopeDefinition().supportsChangeEvents = true;

      if (!defaultRealTime)
         dest.realTime = defaultRealTime;

      if (dest.realTime) {
         // Once the client has fully initialized itself, we'll schedule the first sync to get us connected when in real time mode.
         PTypeUtil.addClientInitJob(new Runnable() {
            void run() {
               scheduleConnectSync(syncDestination.pollTime);
            }
         });
      }
   }

   void scheduleConnectSync(long waitToSyncTime) {
      PTypeUtil.addScheduledJob(new Runnable() {
         void run() {
            // Once we do an initial sync, the response handler will start the next one
            if (syncDestination.numSendsInProgress == 0 && syncDestination.numWaitsInProgress == 0) {
               long now = System.currentTimeMillis();
               if (!sc.dyn.DynUtil.hasPendingJobs() && (lastSentTime == -1 || lastSentTime - now > syncMinDelay)) {
                  autoSync();
               }
               else // wait one more poll interval to do the poll for the sync
                  scheduleConnectSync(syncDestination.pollTime);
            }
         }
      }, waitToSyncTime, false);
   }

   class ClientSyncContext extends SyncManager.SyncContext implements Runnable {
       ClientSyncContext(String name) {
          super(name);
          // On the client, we start recording changes immediately and do not wait for the initial sync
          // request.
          needsInitialSync = false;
       }

       void setNeedsSync(boolean newNeedsSync) {
          super.setNeedsSync(newNeedsSync);
          if (newNeedsSync && autoSync) {
             long delay;
             long nowTime = System.currentTimeMillis();
             long timeSinceLastSend = (nowTime - lastSentTime);
             if (lastSentTime == -1 || timeSinceLastSend > syncMinDelay)
                delay = 0;
             else
                delay = syncMinDelay - timeSinceLastSend;
             PTypeUtil.addScheduledJob(this, delay, false);
          }
       }

       void run() {
          autoSync();
       }
   }

   void autoSync() {
      // If there is no session management for this connection, need to 'reset' on every request to create
      // the session and apply the change. To get real time, effectively every sync request will hold the state
      // and listeners it needs during the 'wait' so it's not entirely stateless. But if the client is not waiting
      // on the server it's like a traditional request/response.
      if (autoSyncDest == null)
         sendSyncToAll(autoSyncGroup, statelessServer, false);
      else {
         sendSync(autoSyncDest, autoSyncGroup, statelessServer, false, null, null);
      }
      lastSentTime = System.currentTimeMillis();
   }

   SyncContext newSyncContext(String name) {
      return new ClientSyncContext(name);
   }

}

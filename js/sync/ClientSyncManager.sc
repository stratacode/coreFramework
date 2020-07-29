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

   boolean sendChangesQueued = false;

   static int currentSyncDelay = -1;    // Set from the syncDelay attribute on text tags. If set to 0
   static boolean syncDelaySet = false;

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

   boolean getNeedsAutoSync() {
      if (syncDestination.numSendsInProgress == 0 && syncDestination.numWaitsInProgress == 0) {
         long now = System.currentTimeMillis();
         if (!sc.dyn.DynUtil.hasPendingJobs() && (lastSentTime == -1 || lastSentTime - now > syncMinDelay)) {
            return true;
         }
      }
      return false;
   }

   class ClientSyncContext extends SyncManager.SyncContext implements Runnable {
       ClientSyncContext(String name) {
          super(name);
          // On the client, we start recording changes immediately and do not wait for the initial sync
          // request.
          needsInitialSync = false;
       }

       void markChanged() {
          // The first time we see a change, schedule an autoSync call to be sure we send it
          super.markChanged();
          if (!sendChangesQueued && autoSync) {
             long absDelay;
             long relDelay;
             // The sync delay to use has been overridden up the stack in an event handler for a specific component
             // use that delay for this event firing only. A value of -1 here means to disable the auto-sync for this
             // particular event, for example to implement a form field that waits till another button is pressed to sync.
             if (syncDelaySet) {
                if (currentSyncDelay == -1)
                   return;
                absDelay = currentSyncDelay;
             }
             else {
                absDelay = syncMinDelay;
             }
             long nowTime = System.currentTimeMillis();
             long timeSinceLastSend = (nowTime - lastSentTime);
             if (lastSentTime == -1 || timeSinceLastSend > absDelay)
                relDelay = 0;
             else
                relDelay = absDelay - timeSinceLastSend;
             sendChangesQueued = true;
             PTypeUtil.addScheduledJob(this, relDelay, false);
          }
       }

       void run() {
          autoSync();
       }
   }

   void autoSync() {
      sendChangesQueued = false;
      // If there is no session management for this connection, need to 'reset' on every request to create
      // the session and apply the change. To get real time, effectively every sync request will hold the state
      // and listeners it needs during the 'wait' so it's not entirely stateless. But if the client is not waiting
      // on the server it's like a traditional request/response.
      if (autoSyncDest == null)
         sendSyncToAll(autoSyncGroup, statelessServer, false);
      else {
         sendSync(autoSyncDest, autoSyncGroup, statelessServer, false, null, null, null);
      }
      lastSentTime = System.currentTimeMillis();
   }

   SyncContext newSyncContext(String name) {
      return new ClientSyncContext(name);
   }

}

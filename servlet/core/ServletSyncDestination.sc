import sc.lang.java.ModelStream;
import sc.lang.java.JavaModel;
import sc.lang.SCLanguage;

import sc.parser.ParseError;
import sc.parser.ParseUtil;

import sc.sync.SyncDestination;
import sc.sync.SyncManager;
import sc.type.IResponseListener;

import sc.layer.LayeredSystem;

import sc.util.StringUtil;

@Component
// Create this on startup and run it before application components 
@CompilerSettings(createOnStartup=true,startPriority=100)
object ServletSyncDestination extends SyncDestination {
   name = "jsHttp"; // The name of the destination in the remote runtime
   defaultScope = "window";

   ServletSyncDestination() {
      super();
   }

   void init() {
      SyncManager.addSyncDestination(this);
   }

   public void writeToDestination(String syncRequestStr, String syncGroup, IResponseListener listener, String paramStr) {
      boolean error = true;
      try {
         Context ctx = Context.getCurrentContext();
         if (paramStr != null) // TODO: use response headers to send and receive these parameters via the XMLHttp call.  I don't think we can send parameters with the initial page sync easily
             System.out.println("*** Warning: ignoring destination params: " + paramStr);
         if (syncRequestStr.length() > 0) {
            ctx.write(SYNC_LAYER_START);
            ctx.write(outputLanguage);
            ctx.write(":");
            ctx.write(syncRequestStr);
         }
         error = false;
      }
      finally {
         if (listener != null) {
            ((SyncListener) listener).completeSync(error);
         }
      }
   }

   public StringBuilder translateSyncLayer(String layerDef) {
      ModelStream stream = ModelStream.convertToModelStream(layerDef);

      if (stream == null)
         return new StringBuilder();
      else {
         boolean trace = SyncManager.trace;

         long startTime = trace ? System.currentTimeMillis() : 0;

         StringBuilder seq = stream.convertToJS(name, "window");

/*
     logged in in SyncDestination.sendSync
         if (trace || SyncManager.verbose)
            System.out.println("Sync reply: size: " + layerDef.length() + " js size: " + seq.length() + " translated in: " + StringUtil.formatFloat((System.currentTimeMillis() - startTime)/1000.0) + " secs\n" +
                              (trace ? layerDef : StringUtil.ellipsis(layerDef, SyncManager.logSize, false)));
         if (SyncManager.traceAll)
            System.out.print("\n\n  --- translated to:\n" + seq.toString() + "\n\n");
*/


         return seq;
      }
   }

   // TODO: Probably need to refactor this for the real time so it takes some context parameter or maybe it needs thread-local?
   public boolean isSendingSync() {
      return false;
   }
}

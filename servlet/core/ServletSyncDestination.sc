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
   remoteRuntimeName = "js"; // For now, we're only talking to the JS runtime.  

   ServletSyncDestination() {
      super();
   }

   void init() {
      SyncManager mgr = SyncManager.addSyncDestination(this);
      // Hook into the synchronization system so we can resolve sync updates for DOM elements - by id for which
      // there is no tag class on the client - the server tags.  A resolver to lookup or create the tag object for
      // this lookup in the sync system if there is one.
      mgr.addFrameworkNameContext(new sc.db.DBSyncNameContext());
   }

   public void writeToDestination(String syncRequestStr, String syncGroup, IResponseListener listener, String paramStr, CharSequence codeUpdates) {
      boolean error = true;
      try {
         Context ctx = Context.getCurrentContext();
         if (paramStr != null) // TODO: use response headers to send and receive these parameters via the XMLHttp call.  I don't think we can send parameters with the initial page sync easily
             System.out.println("*** Warning: ignoring destination params: " + paramStr);
         int syncReqLen = syncRequestStr.length();
         if (syncReqLen > 0) {
            ctx.write(SYNC_LAYER_START);
            ctx.write(outputLanguage);
            ctx.write(":");
            String header = null;
            if (outputLanguage.equals("js")) {
               header = "\n\n//@ sourceURL=scSync.js\n";
               syncReqLen = header.length() + syncReqLen;
            }
            ctx.write(String.valueOf(syncReqLen));
            ctx.write(":");
            if (header != null)
               ctx.write(header);
            ctx.write(syncRequestStr);
         }
         if (codeUpdates != null && codeUpdates.length() > 0) {
            ctx.write(":");
            ctx.write(SYNC_LAYER_START);
            ctx.write("js:"); // TODO: make this configurable?
            ctx.write(String.valueOf(codeUpdates.length()));
            ctx.write(":");
            ctx.write(codeUpdates.toString());
         }
         error = false;
      }
      finally {
         if (listener != null) {
            ((SyncListener) listener).completeSync(error ? SYNC_FAILED_ERROR : null, null);
         }
      }
   }

   public StringBuilder translateSyncLayer(String layerDef) {
      ModelStream stream = ModelStream.convertToModelStream(layerDef, null);

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

   public boolean isClient() {
      return false;
   }
}

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

   ServletSyncDestination() {
      super();
   }

   void init() {
      SyncManager.addSyncDestination(this);
   }

   static ModelStream convertToModelStream(String layerDef) {
      SCLanguage lang = SCLanguage.getSCLanguage();
      boolean trace = SyncManager.trace;
      long startTime = trace ? System.currentTimeMillis() : 0;
      Object streamRes = lang.parseString(layerDef, lang.modelStream);
      if (streamRes instanceof ParseError) {
         ParseError perror = (ParseError) streamRes;
         System.err.println("*** Failed to parse sync layer def: " + perror.errorStringWithLineNumbers(layerDef));
         return null;
      }
      else {
         ModelStream stream = (ModelStream) ParseUtil.nodeToSemanticValue(streamRes);
         stream.setLayeredSystem(LayeredSystem.getCurrent());

         if (trace && layerDef.length() > 2048)
            System.out.println("Parsed sync layer in: " + StringUtil.formatFloat((System.currentTimeMillis() - startTime)/1000.0) + " secs");
         return stream;
      }
   }

   /** Applies the layer definition received from the remote definition.  For Java we'll parse the layer definition and apply it as a set of changes to the instances.  */
   public void applySyncLayer(String layerDef, boolean resetSync) {
      if (layerDef == null || layerDef.length() == 0)
         return;

      ModelStream stream = convertToModelStream(layerDef);

      if (stream != null) {
         boolean trace = SyncManager.trace;
         long startTime = trace ? System.currentTimeMillis() : 0;
         stream.updateRuntime(name, "window", resetSync);
         if (SyncManager.trace)
            System.out.println("Applied sync layer to system in: " + StringUtil.formatFloat((System.currentTimeMillis() - startTime)/1000.0) + " secs");
      }
   }

   public void writeToDestination(String syncRequestStr, String syncGroup, IResponseListener listener, String paramStr) {
      boolean error = true;
      try {
         Context ctx = Context.getCurrentContext();
         if (paramStr != null) // TODO: use response headers to send and receive these parameters via the XMLHttp call.  I don't think we can send parameters with the initial page sync easily
             System.out.println("*** Warning: ignoring destination params: " + paramStr);
         ctx.write(syncRequestStr);
         error = false;
      }
      finally {
         if (listener != null) {
            ((SyncListener) listener).completeSync(error);
         }
      }
   }

   public CharSequence translateSyncLayer(String layerDef) {
      ModelStream stream = convertToModelStream(layerDef);

      if (stream == null)
         return "";
      else {
         boolean trace = SyncManager.trace;
         boolean verbose = SyncManager.verbose;

         long startTime = trace || verbose ? System.currentTimeMillis() : 0;

         CharSequence seq = stream.convertToJS(name, "window");

         if (trace || verbose)
            System.out.println("Sync reply: size: " + layerDef.length() + " js size: " + seq.length() + " translated in: " + StringUtil.formatFloat((System.currentTimeMillis() - startTime)/1000.0) + " secs\n" +
                              (SyncManager.traceAll ? layerDef : StringUtil.ellipsis(layerDef, SyncManager.logSize, false)));
         if (verbose)
            System.out.print("\n\n  --- translated to:\n" + seq.toString() + "\n\n");


         return seq;
      }
   }

   // TODO: Probably need to refactor this for the real time so it takes some context parameter or maybe it needs thread-local?
   public boolean isSendingSync() {
      return false;
   }
}

// When you extend this layer, your code will run on the client if there's no server 
// and on the server if there is.  Extend this layer from your data layers.   If data layers
// run on both client and server, they introduce conflicts in the synchronization mechanism - because we define the data
// on both sides and don't choose a single origin for the data.  In the best case, it's just duplicate effort but sometimes you'll actually multiple copies of the same record 
// This happens when there's no unique id shared beteen objects created with the "new" operator.. as there's no way to register a correspondence between different copies of
// the same record.
js.clientOrServer extends js.core {
   compiledOnly = true;
   public void init() {
      if (layeredSystem.getLayerByDirName("servlet.webApp") != null) {
         // Exclude the javascript, android, and gwt runtimes.  All layers which extend this layer explicitly will also be excluded, unless they explicitly include a layer which uses JS
         excludeRuntimes("js", "android", "gwt");

         // When used with the servlet.webApp add a dependency so we run on the server only.
         addRuntime(null);
      }
   }
}

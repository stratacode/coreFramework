// When you extend this layer, your code will run on the client if there's no server 
// and on the server if there is.  Extend this layer from your data layers.   If data layers
// run on both client and server, they introduce conflicts in the synchronization mechanism.  In the best case, it's just duplicate effort but sometimes you'll actually get duplicates - like when there's no unique id shared beteen objects you create with "new".
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

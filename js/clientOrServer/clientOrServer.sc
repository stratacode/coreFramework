// When you extend this layer, code in this layer will run on the client in client-only mode
// and on the server-only in client/server mode.  You should especially extend this layers like data layers used to initialize
// values since the client will be automatically initialized with the server's version, and will keep them in sync after that.
// If you initialize properties on both client and server it can introduce conflicts during synchronization.  Because we define the data
// on both sides and don't choose a single origin for the data, after you start changing it conflicts arise.  In the best case, it's effort duplicated
// but it can cause bugs due to having records duplicated.  This happens when there's no unique id shared beteen objects created with the "new"
// operator.. as there's no way to register a correspondence between different copies of the same record.
js.clientOrServer extends js.core {
   compiledOnly = true;

   codeType = sc.layer.CodeType.Framework;

   public void init() {
      if (layeredSystem.getLayerByDirName("servlet.webApp") != null) {
         // Exclude the javascript, android, and gwt runtimes.  All layers which extend this layer explicitly will also be excluded, unless they explicitly include a layer which uses JS
         excludeRuntimes("js", "android", "gwt");

         // When used with the servlet.webApp add a dependency so we run on the server only.
         addRuntime(null);
      }
   }
}

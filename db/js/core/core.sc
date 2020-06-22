/** DB support for JS - as a client to a database */
public db.js.core {
   compiledOnly = true;
   codeType = sc.layer.CodeType.Framework;
   hidden = true;

   void init() {
      includeRuntime("js");
   }
   void start() {
      DBProvider provider = new DBProvider("jsclient");
      provider.needsGetSet = false;
      provider.needsSchema = false;
      provider.needsMetadata = false;
      provider.needsQueryMethods = false;
      layeredSystem.addDBProvider(provider, this);
      layeredSystem.defaultDBProvider = provider;
   }
}

package sc;

/** This layer defines JS versions of server types used for manipulating layers. */
@sc.js.JSSettings(jsModuleFile="js/sclayer.js", prefixAlias="sc_")
public js.layer extends js.schtml, js.sync {
   exportPackage = false;

   codeType = sc.layer.CodeType.Framework;

   compiledOnly = true;

   finalLayer = true;
   buildLayer = true;

   void start() {
      if (activated) // this sync class is not easily discoverable due to replaceWith so added this hook to make it pass the syncTypeFilter
         sc.sync.SyncManager.addGlobalSyncTypeName("sc.lang.JLineInterpreter");
   }
}

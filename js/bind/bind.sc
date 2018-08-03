package sc.js;

@sc.js.JSSettings(jsModuleFile="js/scbind.js", requiredModule=true, prefixAlias="sc_")
public js.bind extends js.core {
   codeType = sc.layer.CodeType.Framework;
   hidden = true;

   compiledOnly = true;
   finalLayer = true;
}

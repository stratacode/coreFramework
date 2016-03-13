package sc.js;

@sc.js.JSSettings(jsModuleFile="js/scbind.js", requiredModule=true)
public js.bind extends js.core {
   codeType = sc.layer.CodeType.Framework;
   codeFunction = sc.layer.CodeFunction.Program;
   hidden = true;

   compiledOnly = true;
   finalLayer = true;
}

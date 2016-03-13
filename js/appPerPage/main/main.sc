package sc.tag;

@sc.js.JSSettings(jsModuleFile="js/taggen.js", requiredModule=true)
public js.appPerPage.main extends meta {
   exportPackage = false;
   codeType = sc.layer.CodeType.Framework;
   codeFunction = sc.layer.CodeFunction.Program;

   compiledOnly = true;

   // Need the build layer to be after we install the tag classes - or somehow ensure upstream
   // tag classes don't get injected into downstream layers
   buildLayer = true;
}

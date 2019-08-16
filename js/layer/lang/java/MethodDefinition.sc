import sc.obj.GetSet;

@sc.js.JSSettings(prefixAlias="sc_",jsModuleFile="js/sclayer.js")
class MethodDefinition extends AbstractMethodDefinition {
   @GetSet
   String returnTypeName;
   @GetSet
   String propertyName;
}
import sc.obj.GetSet;
import sc.obj.IObjectId;
import sc.dyn.DynUtil;

@sc.js.JSSettings(prefixAlias="sc_",jsModuleFile="js/sclayer.js")
class Parameter implements IObjectId {
   @GetSet
   String variableName;

   @GetSet
   String parameterTypeName;

   String getObjectId() {
      return DynUtil.getObjectId(this, null, "PMD_" + parameterTypeName  + "_" + variableName);
   }
}
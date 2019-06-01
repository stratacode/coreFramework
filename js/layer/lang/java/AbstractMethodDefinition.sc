import java.util.List;
import sc.obj.GetSet;
import sc.obj.IObjectId;
import sc.dyn.DynUtil;

@sc.js.JSSettings(prefixAlias="sc_",jsModuleFile="js/sclayer.js")
abstract class AbstractMethodDefinition extends Statement implements IObjectId {
   @GetSet
   String name;

   @GetSet
   List<Parameter> parameterList;

   @GetSet
   String comment;

   @GetSet
   String methodTypeName;

   // Must stay in sync with the server version
   String getObjectId() {
      String methName = name == null ? "_init_" : name;
      return DynUtil.getObjectId(this, null, "MMD_" + methodTypeName  + "_" + methName);
   }
}
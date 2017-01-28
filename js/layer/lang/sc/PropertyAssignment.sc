import sc.layer.Layer;
import sc.lang.java.Statement;
import sc.lang.java.IVariableInitializer;
import sc.dyn.DynUtil;
import sc.obj.GetSet;

@sc.js.JSSettings(prefixAlias="sc_",jsModuleFile="js/sclayer.js")
class PropertyAssignment extends Statement implements IVariableInitializer, sc.obj.IObjectId {
   String propertyName;
   @Bindable
   String operatorStr;
   @Bindable
   String initializerExprStr;
   @GetSet
   String variableTypeName;
   @GetSet
   Layer layer;

   String getVariableName() {
      return propertyName;
   }

   @GetSet
   String comment;
   //String modifiers;

   // TODO: this is replicated on the server in the real PropertyAssignment but hopefully it never gets used since
   // we assign sync ids when these are allocated on the server.
   String getObjectId() {
      String typeName = variableTypeName == null ? "null" : sc.type.CTypeUtil.getClassName(variableTypeName);
      return DynUtil.getObjectId(this, null, "MD_" + typeName  + "_" + propertyName);
   }
}

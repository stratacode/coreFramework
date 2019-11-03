import sc.layer.Layer;
import sc.obj.GetSet;

class VariableDefinition extends Statement implements IVariableInitializer {
   @GetSet
   String variableName;
   @Bindable
   String initializerExprStr;
   @Bindable
   String operatorStr;
   @GetSet
   Layer layer;
   @GetSet
   String variableTypeName;
   @GetSet
   boolean indexedProperty;
   @GetSet
   boolean writable;
}

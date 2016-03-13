import sc.layer.Layer;

class VariableDefinition implements IVariableInitializer {
   @sc.obj.GetSet
   String variableName;
   @Bindable
   String initializerExprStr;
   @Bindable
   String operatorStr;
   @sc.obj.GetSet
   Layer layer;
   @sc.obj.GetSet
   String comment;
   @sc.obj.GetSet
   String variableTypeName;
}

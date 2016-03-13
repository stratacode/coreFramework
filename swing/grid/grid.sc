
public swing.grid extends gui.grid, swing.core {
   compiledOnly = true;

   codeType = sc.layer.CodeType.Framework;
   codeFunction = sc.layer.CodeFunction.UI;

   public void start() {
      sc.layer.LayeredSystem system = getLayeredSystem();
      sc.lang.sc.BasicScopeProcessor listItemScope = new sc.lang.sc.BasicScopeProcessor("ListItem");
      listItemScope.validOnClass = false;
      listItemScope.validOnField = false;
      listItemScope.validOnObject = true;
      listItemScope.objectTemplate= "sc.gui.grid.ListItemScope";
      //listItemScope.contextParams = "<%= isAssignableTo(\"org.apache.wicket.Component\") ? %>\"<%=lowerClassName%>\",<% : \"\" %>listItemIndex,listItemModel";
      listItemScope.useNewTemplate = true;
      listItemScope.appendInterfaces = new String[] {"sc.dyn.IObjChildren", "sc.gui.grid.IListItemScope<T>"};
      listItemScope.requiredParentType = "sc.gui.grid.ListView";
      listItemScope.childGroupName = "ListItem"; // Children with this scope are not included in regular childreNames list - in childScopesByName under the key ListItem instead.
      registerScopeProcessor("ListItem", listItemScope);
   }
}

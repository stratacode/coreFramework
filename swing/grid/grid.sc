
public swing.grid extends gui.grid, swing.core {
   compiledOnly = true;

   codeType = sc.layer.CodeType.Framework;

   public void start() {
      sc.layer.LayeredSystem system = getLayeredSystem();
      sc.lang.sc.BasicScopeProcessor listItemScope = new sc.lang.sc.BasicScopeProcessor("ListItem");
      listItemScope.validOnClass = false;
      listItemScope.validOnField = false;
      listItemScope.validOnObject = true;
      // TODO: the ListItemScope object template adds a bunch of methods which are needed by the appendInterfaces.  We either could add 
      // new capabilities to the dyn object manager or just live with these types being compiled only.  But right now compiledOnly as an inner
      // class breaks when the outer type is dynamic (see test.dynInnerComp)
      //listItemScope.compiledOnly = true;
      listItemScope.objectTemplate= "sc.gui.grid.ListItemScope";
      //listItemScope.contextParams = "<%= isAssignableTo(\"org.apache.wicket.Component\") ? %>\"<%=lowerClassName%>\",<% : \"\" %>listItemIndex,listItemModel";
      listItemScope.useNewTemplate = true;
      listItemScope.appendInterfaces = new String[] {"sc.dyn.IObjChildren", "sc.gui.grid.IListItemScope<T>"};
      listItemScope.requiredParentType = "sc.gui.grid.ListView";
      listItemScope.childGroupName = "ListItem"; // Children with this scope are not included in regular childreNames list - in childScopesByName under the key ListItem instead.
      registerScopeProcessor("ListItem", listItemScope);
   }
}

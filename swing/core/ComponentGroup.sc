public class ComponentGroup<T> extends ComponentList<T> implements IChildContainer {
   public boolean visible = true;
   private java.awt.Component parentComponent;
   visible =: propagateVisible();

   public Object[] getChildren() {
      ArrayList<Object> res = new ArrayList<Object>();
      for (int i = 0; i < size(); i++) {
         Object c = get(i);
         if (c instanceof java.awt.Component || c instanceof IChildContainer)
            res.add(c);
      }
      return res.toArray(new Object[res.size()]);
   }

   void propagateVisible() {
       if (parentComponent != null && parentComponent.visible != visible)
          return;

       Object[] children = getChildren();
       if (children == null)
          return;
       for (Object child:children) {
          if (child instanceof java.awt.Component)
             ((java.awt.Component)child).setVisible(visible);
          // TODO: really should check for IChildContainer here and propagate the visibility to the results of getChildren
          else if (child instanceof ComponentGroup) {
             ((ComponentGroup) child).visible = visible;
          }
       }
   }

   public void setParentComponent(java.awt.Component component) {
      parentComponent = component;
   }

   public java.awt.Component getParentComponent() {
     return parentComponent; 
   }
}

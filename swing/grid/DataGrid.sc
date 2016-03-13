DataGrid implements IChildContainer {
   private java.awt.Component parentComponent;
   void setParentComponent(java.awt.Component comp) {
      parentComponent = comp;
   }

   java.awt.Component getParentComponent() {
      return parentComponent;
   }

   /** Allows the grid to be used as a child object of a UIPanel or other container.  Bubbles up the actual JComponent children from panel generated. */
   JComponent[] getChildren() {
      JComponent[] res = new JComponent[1];
      res[0] = getRootPanel();
      return res;
   }
}

import javax.swing.JComponent;

GridElement {
   JComponent widgetParent;

   JComponent getRootPanel() {
      if (parent == null) {
         if (widgetParent != null)
            return widgetParent;

         widgetParent = new JPanel();
         return widgetParent;
      }
      else 
         return parent.getRootPanel();

   }
}

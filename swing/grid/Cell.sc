import javax.swing.JComponent;

Cell {
   void updateLayout() {
      // This method first updates the parent/child relationship if necessary.  The idea being that most UI toolkits
      // do refresh as a batch so right before we adjust the size, we'll fix the parent hierarchy so it transitions smoothly
      Object curParent = getRootPanel();
      if (curParent != widgetParent) {
         if (widgetParent instanceof JComponent && children != null) {
            for (Object child:children) {
               if (child instanceof JComponent) 
                  widgetParent.remove((JComponent) child);
            }
            widgetParent = (JComponent) curParent;
            for (Object child:children) {
               if (child instanceof JComponent) 
                  widgetParent.add((JComponent) child);
            }
         }
      }
      super.updateLayout();
   }

   /* Pulls the min width out of the child widget provided.  Returns -1 if there is no assigned min width or not supported by the toolkit */
   int getChildMinWidth(Object child) {
      return (int) ((JComponent) child).minimumSize.width;
   }

   int getChildMaxHeight(Object child) {
      return (int) ((JComponent) child).maximumSize.getHeight();
   }

   int getChildMinHeight(Object child) {
      return (int) ((JComponent) child).minimumSize.getHeight();
   }

   int getChildMaxWidth(Object child) {
      return (int) ((JComponent) child).maximumSize.getWidth();
   }

   int getChildPreferredWidth(Object child) {
      return (int) ((JComponent) child).preferredSize.getWidth();
   }

   int getChildPreferredHeight(Object child) {
      return (int) ((JComponent) child).preferredSize.getHeight();
   }

   boolean getChildVisible(Object child) {
      return ((JComponent) child).visible;
   }

   void setChildDimensions(Object child, int childX, int childY, int childW, int childH) {
      JComponent comp = (JComponent) child;
      comp.setSize(childW, childH);
      comp.setLocation(childX, childY);
   }
}

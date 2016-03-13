@Component
// Do not do children for the SplitPane... it only supports the top and bottom components anyway.
//@CompilerSettings(objectTemplate="javax.swing.JComponentInit", newTemplate="javax.swing.JComponentNew", dynChildManager="sc.swing.SwingDynChildManager")
public class JSplitPane extends javax.swing.JSplitPane {
   static sc.type.IBeanMapper sizeProperty = sc.type.TypeUtil.getPropertyMapping(JSplitPane.class, "size");
   static sc.type.IBeanMapper locationProperty = sc.type.TypeUtil.getPropertyMapping(JSplitPane.class, "location");
   static sc.type.IBeanMapper dividerPosition = sc.type.TypeUtil.getPropertyMapping(JSplitPane.class, "dividerPosition");

   override @Bindable size;
   override @Bindable topComponent;

   /** There are two setX methods for dividerLocation so it's not a real property in the JavaBean style so we use dividerPosition here */
   @Bindable(manual=true)
   public void setDividerPosition(int pixels) {
      setDividerLocation(pixels);
      sc.bind.Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, this, dividerPosition);
   }

   public int getDividerPosition() {
      return getDividerLocation();
   }

   public void setBounds(int x, int y, int w, int h) {
      boolean sizeChanged = this.width != w || this.height != h;
      boolean locChanged = this.x != x || this.y != y;
      super.setBounds(x, y, w, h);
      if (sizeChanged)
         sc.bind.Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, this, sizeProperty);
      if (locChanged)
         sc.bind.Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, this, locationProperty);
   }
}

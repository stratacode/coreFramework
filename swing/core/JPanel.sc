/** Wraps javax.swing.JPanel to make it a bindable StrataCode component, where child objects become managed child widgets */
@Component
@CompilerSettings(objectTemplate="javax.swing.JComponentInit", newTemplate="javax.swing.JComponentNew", dynChildManager="sc.swing.SwingDynChildManager")
public class JPanel extends javax.swing.JPanel {
   static sc.type.IBeanMapper sizeProperty = sc.type.TypeUtil.getPropertyMapping(JPanel.class, "size");
   static sc.type.IBeanMapper locationProperty = sc.type.TypeUtil.getPropertyMapping(JPanel.class, "location");

   layout = null;

   override @Bindable size;
   override @Bindable location;
   override @Bindable visible;

   override @Bindable preferredSize;
   override @Bindable font;

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

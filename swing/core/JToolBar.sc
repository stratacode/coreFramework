@Component
@CompilerSettings(objectTemplate="javax.swing.JComponentInit", newTemplate="javax.swing.JComponentNew", dynChildManager="sc.swing.SwingDynChildManager", inheritProperties=false)
public class JToolBar extends javax.swing.JToolBar {
   static sc.type.IBeanMapper sizeProperty = sc.type.TypeUtil.getPropertyMapping(JToolBar.class, "size");
   static sc.type.IBeanMapper locationProperty = sc.type.TypeUtil.getPropertyMapping(JToolBar.class, "location");

   override @Bindable size;

   override @Bindable location;
 
   size =: doLayout();

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

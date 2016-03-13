import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

@Component
public class JCheckBoxMenuItem extends javax.swing.JCheckBoxMenuItem implements ComponentStyle {
   private static sc.type.IBeanMapper selectedProp = sc.type.TypeUtil.getPropertyMapping(JCheckBoxMenuItem.class, "selected");
   {
      addItemListener(new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
             SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JCheckBoxMenuItem.this, selectedProp);
          }});
   }

   override @Bindable selected;
}

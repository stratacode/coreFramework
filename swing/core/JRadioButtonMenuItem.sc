import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

@Component
public class JRadioButtonMenuItem extends javax.swing.JRadioButtonMenuItem implements ComponentStyle {
   private static sc.type.IBeanMapper selectedProp = sc.type.TypeUtil.getPropertyMapping(JRadioButtonMenuItem.class, "selected");
   {
      addItemListener(new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
             SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JRadioButtonMenuItem.this, selectedProp);
          }});
   }

   override @Bindable selected;
}

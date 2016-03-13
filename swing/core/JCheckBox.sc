import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import sc.type.IBeanMapper;

@Component
public class JCheckBox extends javax.swing.JCheckBox implements ComponentStyle {
   private static IBeanMapper selectedProp = sc.type.TypeUtil.getPropertyMapping(JCheckBox.class, "selected");

   override @Bindable size;
   override @Bindable(manual=true) preferredSize;
   override @Bindable location;
   override @Bindable visible;

   {
      addItemListener(new ItemListener() {
         /** Listen to the slider. */
          public void itemStateChanged(ItemEvent e) {
             SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JCheckBox.this, selectedProp);
          }});
   }

   @Bindable(manual = true)
   public void setText(String text) {
      super.setText(text);
      sc.bind.Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, this, SwingUtil.preferredSizeProp);
   }
}

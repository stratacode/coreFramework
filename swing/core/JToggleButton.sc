import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import sc.type.IBeanMapper;

@Component
public class JToggleButton extends javax.swing.JToggleButton implements ComponentStyle {
   private static IBeanMapper selectedProp = sc.type.TypeUtil.getPropertyMapping(JToggleButton.class, "selected");
   private static IBeanMapper preferredSizeProp = sc.type.TypeUtil.getPropertyMapping(JToggleButton.class, "preferredSize");
   {
      addItemListener(new ItemListener() {
         /** Listen to the slider. */
          public void itemStateChanged(ItemEvent e) {
             SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JToggleButton.this, selectedProp);
          }});
   }

   @Bindable
   public void setText(String text) {
      super.setText(text);
      sc.bind.Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, this, preferredSizeProp);
   }

   @Bindable
   public void setIcon(Icon icon) {
      super.setIcon(icon);
      sc.bind.Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, this, preferredSizeProp);
   }
}

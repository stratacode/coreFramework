import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

@CompilerSettings(inheritProperties=false)
@Component
public class JRadioButton extends javax.swing.JRadioButton implements ComponentStyle {
   private static sc.type.IBeanMapper selectedProp = sc.type.TypeUtil.getPropertyMapping(JRadioButton.class, "selected");
   private static sc.type.IBeanMapper preferredSizeProp = sc.type.TypeUtil.getPropertyMapping(JRadioButton.class, "preferredSize");
   {
      addItemListener(new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
             SwingUtil.updateUserAction();
             SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JRadioButton.this, selectedProp);
          }});
   }

   override @Bindable preferredSize;
   override @Bindable selected;
   override @Bindable location;
   override @Bindable size;
   override @Bindable visible;

   @Bindable
   public void setText(String text) {
      super.setText(text);
      sc.bind.Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, this, preferredSizeProp);
   }
}

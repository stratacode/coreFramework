import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import sc.type.IBeanMapper;

@Component
public class JSlider extends javax.swing.JSlider implements ComponentStyle {

   private boolean inSet = false;

   private final static int UNSET_VALUE = Integer.MIN_VALUE;

   private int lastValue = UNSET_VALUE;

   @Bindable(manual=true)
   public void setValue(int val) {
      if (sc.dyn.DynUtil.equalObjects(getValue(), val))
         return;
      try {
         inSet = true;
         super.setValue(val);
         if (val != lastValue || lastValue == UNSET_VALUE)
            SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JSlider.this, valueProp);
         lastValue = val;
      }
      finally {
         inSet = false;
      }
   }

   private static IBeanMapper valueProp = sc.type.TypeUtil.getPropertyMapping(JSlider.class, "value");
   {
      addChangeListener(new ChangeListener() {
         /** Listen to the slider. */
          public void stateChanged(ChangeEvent e) {
             // These events get send even when the value has not changed
             if (!inSet && (lastValue == UNSET_VALUE || getValue() != lastValue)) {
                SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JSlider.this, valueProp);
                lastValue = getValue();
             }
          }});
   }
}

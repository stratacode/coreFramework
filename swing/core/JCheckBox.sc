import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import sc.type.IBeanMapper;

import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;

@CompilerSettings(inheritProperties=false)
@Component
public class JCheckBox extends javax.swing.JCheckBox implements ComponentStyle {
   private static IBeanMapper selectedProp = sc.type.TypeUtil.getPropertyMapping(JCheckBox.class, "selected");

   override @Bindable size;
   override @Bindable(manual=true) preferredSize;
   override @Bindable location;
   override @Bindable visible;

   /** This event is fired whenever the user changes the text value - not when text is changed otherwise */
   @Bindable
   public int userEnteredCount = 0;

   @Bindable
   public boolean focus = false;

   {
      addItemListener(new ItemListener() {
         /** Listen to the checkbox. */
          public void itemStateChanged(ItemEvent e) {
             SwingUtil.updateUserAction();
             userEnteredCount++;
             SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JCheckBox.this, selectedProp);
          }});
   }

   @Bindable(manual = true)
   public void setText(String text) {
      super.setText(text);
      sc.bind.Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, this, SwingUtil.preferredSizeProp);
   }

   {
      addFocusListener(new FocusListener() {
         public void focusGained(FocusEvent e) {
            focus = true;
         }

         public void focusLost(FocusEvent e) {
            focus = false;
         }
      });
   }
}

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import sc.type.IBeanMapper;

@Component
public class JToggleButton extends javax.swing.JToggleButton implements ComponentStyle {
   private static IBeanMapper selectedProp = sc.type.TypeUtil.getPropertyMapping(JToggleButton.class, "selected");
   private static IBeanMapper userSelectedProp = sc.type.TypeUtil.getPropertyMapping(JToggleButton.class, "userSelected");
   private static IBeanMapper preferredSizeProp = sc.type.TypeUtil.getPropertyMapping(JToggleButton.class, "preferredSize");

   {
      addItemListener(new ItemListener() {
         /** Listen to the selected state change event. */
          public void itemStateChanged(ItemEvent e) {
             SwingUtil.updateUserAction();
             //boolean newSel = isSelected();
             //if (lastSelected == null || newSel != lastSelected) {
             //   lastSelected = newSel;
                SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JToggleButton.this, selectedProp);
             //}
          }});

      /** Listen to the user action which changes the selected state state. This is immediate so the mouseEvent is set in the event handler */
      addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            SwingUtil.updateUserAction();
            Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, JToggleButton.this, userSelectedProp);
               //SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JToggleButton.this, selectedProp);
         }
      });
   }

   /** The current mouse event (or null). It will only be set for the duration of the event - all immediate bindings listening to the setSelected method */
   public MouseEvent mouseEvent = null;

   @Bindable
   public void setText(String text) {
      super.setText(text);
      sc.bind.Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, this, preferredSizeProp);
   }

   public boolean getUserSelected() {
      return isSelected();
   }

   public void setUserSelected(boolean v) {
      setSelected(v);
   }

   @Bindable
   public void setIcon(Icon icon) {
      super.setIcon(icon);
      sc.bind.Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, this, preferredSizeProp);
   }

   protected void processMouseEvent(MouseEvent e) {
      mouseEvent = e;
      try {
         super.processMouseEvent(e);
      }
      finally {
         mouseEvent = null;
      }
   }
}

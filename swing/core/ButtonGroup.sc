import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.AbstractButton;

@Component
public class ButtonGroup extends javax.swing.ButtonGroup {
   static sc.type.IBeanMapper selectionProperty = sc.type.TypeUtil.getPropertyMapping(ButtonGroup.class, "selection");
   
   ItemListener buttonItemListener = new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
	 if (e.getStateChange() == ItemEvent.SELECTED) {
	    Object selectedButton = e.getItem();
	    int i = 0;
	    for (Enumeration buttons = getElements(); buttons.hasMoreElements(); i++) {
	       if (buttons.nextElement().equals(selectedButton)) {
		  selectedIndex = i;
		  break;
	       }
	    }
	 }
         SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, ButtonGroup.this, selectionProperty);
      }
   };

   private int _selectedIndex = -1;

   @Bindable
   public void setSelectedIndex(int ix) {
      _selectedIndex = ix;
      if (ix == -1)
	 super.clearSelection();
      else {
         int bix = 0;
         for (Enumeration buttons = getElements(); buttons.hasMoreElements(); bix++) {
            AbstractButton butt = (AbstractButton) buttons.nextElement();
            if (bix == ix) {
               setSelected(butt.getModel(), true);
               break;
            }
         }
      }
   }

   public int getSelectedIndex() {
      return _selectedIndex;
   }

   // A list of AbstractButtons which are the elements of this ButtonGroup
   private java.util.List<AbstractButton> _buttons;
   @Bindable
   public java.util.List<AbstractButton> getButtons() {
      return _buttons;
   }
   public void setButtons(java.util.List<AbstractButton> _el) {
      if (_buttons != null && _inited) {
         for (AbstractButton button:_buttons) {
            if (button != null) {
               remove(button);
               button.removeItemListener(buttonItemListener);
            }
	     }
      }
      _buttons = _el;
      if (_buttons != null && _inited) {
         for (AbstractButton button:_buttons) {
            if (button != null) {
               add(button);
               button.addItemListener(buttonItemListener);
            }
            else {
               System.err.println("*** null button in the array passed to setButtons in ButtonGroup");
            }
         }
      }
   }

   private boolean _inited = false;

   public void init() {
      _inited = true;
      if (_buttons != null) {
         for (AbstractButton button:_buttons) {
	    add(button);
	    button.addItemListener(buttonItemListener);
	 }	    
      }
   }

   public void clearSelection() {
      selectedIndex = -1;
   }
}

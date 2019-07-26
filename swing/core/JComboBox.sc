import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.DefaultListCellRenderer;
import sc.type.TypeUtil;
import sc.type.IBeanMapper;

@Component
public class JComboBox extends javax.swing.JComboBox implements ComponentStyle {
   static IBeanMapper selectedItemProperty = TypeUtil.getPropertyMapping(JComboBox.class, "selectedItem");
   static IBeanMapper selectedIndexProperty = TypeUtil.getPropertyMapping(JComboBox.class, "selectedIndex");
   static IBeanMapper userSelectedItemProperty = TypeUtil.getPropertyMapping(JComboBox.class, "userSelectedItem");
   static IBeanMapper preferredSizeProperty = TypeUtil.getPropertyMapping(JComboBox.class, "preferredSize");
   {
      // Swing does not support binding events on its text property.
      addItemListener(new ItemListener() {
         public void itemStateChanged(ItemEvent e) {
            // Avoid the duplicate event when we are called from the setter and the selected/deselected events
            if (!inSet && e.stateChange == ItemEvent.SELECTED) {
               SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JComboBox.this, selectedItemProperty);
            }
            // The setSelectedIndex -> setSelectedItem call is used both programmatically and by the UI so we need some way to
            // differentiate... probably need keyboard context here as well?
            if (userSelectedIndex) {
               userSelectedItem = selectedItem;
               SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JComboBox.this, userSelectedItemProperty);
            }
         }
      });

   }

   renderer = new BackgroundListRenderer();

   private java.util.List _items;
   private boolean _inited;

   private boolean inSet = false;
   private boolean notUserSet = false;
   private boolean userSelectedIndex = false;

   private Integer pendingSelectedIndex = null;

   override @Bindable preferredSize;
   override @Bindable location;

   public @Bindable(manual=true) Object userSelectedItem;

   @Bindable(manual=true)
   public void setSelectedItem(Object item) {
      if (sc.dyn.DynUtil.equalObjects(item, getSelectedItem()))
         return;
      try {
         inSet = true;
         super.setSelectedItem(item);
         SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JComboBox.this, selectedItemProperty);
      }
      finally {
         inSet = false;
      }
   }

   @Bindable
   public void setItems(java.util.List newItems) {
      _items = newItems;
      if (_inited) {
         int selIndex = selectedIndex;
         Object selItem = selectedItem;
         removeAllItems();
         int i = 0;
         int newSelIx = -1;
         for (Object item:_items) {
            addItem(item);
            if (item == selItem)
                newSelIx = i;
            i++;
         }
         if (newSelIx != -1)
           setSelectedIndexNotUser(newSelIx);
         else if (selectedIndex >= _items.size())
           setSelectedIndexNotUser(_items.size() - 1);
      }
      SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JComboBox.this, preferredSizeProperty);
   }

   public java.util.List getItems() {
      return _items;
   }

   items =: checkBounds();

   private void checkBounds() {
      if (selectedIndex >= _items.size())
        setSelectedIndexNotUser(_items.size() - 1);
   }

   public void init() {
      _inited = true;
      if (_items != null) {
         for (Object item:_items)
            addItem(item);
      }
      if (pendingSelectedIndex != null && pendingSelectedIndex != getSelectedIndex()) {
         setSelectedIndexNotUser(pendingSelectedIndex);
      }
   }

   @Bindable
   public void setSize(Dimension d) {
      super.setSize(d);
      invalidate();
      validate();
   }

   // Because swing calls setSelectedIndex directly from the mouse handler, there's no way to really tell
   // whether or not it's a user generated event, so we default to the call being user generated and make sure all
   // code generated events use this method.
   public void setSelectedIndexNotUser(int index) {
      try {
         notUserSet = true;
         setSelectedIndex(index);
      }
      finally {
         notUserSet = false;
      }
   }

   @Bindable(manual=true)
   public void setSelectedIndex(int index) {
      if (!_inited) {
         pendingSelectedIndex = index;
         return;
      }
      else
         pendingSelectedIndex = null;
      try {
         if (!notUserSet)
            userSelectedIndex = true;
         if (items != null && index < items.size())
            super.setSelectedIndex(index);
         SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JComboBox.this, selectedIndexProperty);
      }
      finally {
         userSelectedIndex = false;
      }
   }

   public int getSelectedIndex() {
      return super.getSelectedIndex();
   }
}

class BackgroundListRenderer extends DefaultListCellRenderer {

   public java.awt.Component getListCellRendererComponent(
                                           JList list,
                                           Object value,
                                           int index,
                                           boolean isSelected,
                                           boolean cellHasFocus) {
      java.awt.Component ret = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (isSelected) {
         setBackground(list.getSelectionBackground());
         setForeground(list.getSelectionForeground());
      } 
      else {
         setBackground(list.getBackground());
         setForeground(list.getForeground());
      }

      return ret;
  }
}

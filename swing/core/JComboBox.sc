import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.DefaultListCellRenderer;

@Component
public class JComboBox extends javax.swing.JComboBox implements ComponentStyle {
   static sc.type.IBeanMapper selectedItemProperty = sc.type.TypeUtil.getPropertyMapping(JComboBox.class, "selectedItem");
   static sc.type.IBeanMapper selectedIndexProperty = sc.type.TypeUtil.getPropertyMapping(JComboBox.class, "selectedIndex");
   static sc.type.IBeanMapper userSelectedItemProperty = sc.type.TypeUtil.getPropertyMapping(JComboBox.class, "userSelectedItem");
   static sc.type.IBeanMapper preferredSizeProperty = sc.type.TypeUtil.getPropertyMapping(JComboBox.class, "preferredSize");
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
            if (inSelectedIndex) {
               SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JComboBox.this, userSelectedItemProperty);
            }
         }
      });

   }

   renderer = new BackgroundListRenderer();

   private java.util.List _items;
   private boolean _inited;

   private boolean inSet = false;
   private boolean inSelectedIndex = false;

   override @Bindable preferredSize;
   override @Bindable location;

   public @Bindable(manual=true) Object userSelectedItem := selectedItem;

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
           selectedIndex = newSelIx;
         else if (selectedIndex >= _items.size())
           selectedIndex = _items.size() - 1;
      }
      SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JComboBox.this, preferredSizeProperty);
   }

   public java.util.List getItems() {
      return _items;
   }

   items =: checkBounds();

   private void checkBounds() {
      if (selectedIndex >= _items.size())
        selectedIndex = _items.size() - 1;
   }

   public void init() {
      _inited = true;
      if (_items != null) {
         for (Object item:_items)
            addItem(item);
      }
   }

   @Bindable
   public void setSize(Dimension d) {
      super.setSize(d);
      invalidate();
      validate();
   }

   @Bindable(manual=true)
   public void setSelectedIndex(int index) {
      try {
         inSelectedIndex = true;
         super.setSelectedIndex(index);
         SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JComboBox.this, selectedIndexProperty);
      }
      finally {
         inSelectedIndex = false;
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

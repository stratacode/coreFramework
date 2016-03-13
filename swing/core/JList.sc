import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

@Component
public class JList extends javax.swing.JList {
   static sc.type.IBeanMapper selectedIndexProperty = sc.type.TypeUtil.getPropertyMapping(JList.class, "selectedIndex");
   static sc.type.IBeanMapper selectedValueProperty = sc.type.TypeUtil.getPropertyMapping(JList.class, "selectedValue");
   {
      addListSelectionListener(new ListSelectionListener() {
         public void valueChanged(ListSelectionEvent e) {
	    if (e.getValueIsAdjusting())
	       return;
	    SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JList.this, selectedIndexProperty);
	    SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JList.this, selectedValueProperty);
         }
      });
   }

   private java.util.List _listItems;

   public void setListItems(java.util.List listItems) {
      _listItems = listItems;
      setListData((listItems == null) ? new Object[0] : listItems.toArray());
   }

   public java.util.List getListItems() {
      return _listItems;
   }

   @Bindable
   public void setSelectedValue(Object selectedValue) {
      setSelectedValue(selectedValue, true);
   }

   @Bindable
   public void setSelectedIndex(int index) {
      super.setSelectedIndex(index);
      if (index == -1) 
	 clearSelection();
   }
}

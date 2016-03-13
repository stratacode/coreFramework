import sc.bind.*;
import sc.type.*;
import android.view.View;

class ListSelectListener implements AdapterView.OnItemSelectedListener {
   static IBeanMapper[] notifyProps = new IBeanMapper[] {
          TypeUtil.getPropertyMapping(AdapterView.class, "selectedItem"), 
          TypeUtil.getPropertyMapping(AdapterView.class, "selectedItemId"), 
          TypeUtil.getPropertyMapping(AdapterView.class, "selectedItemPosition")}; 

   static sc.type.IBeanMapper textProperty = sc.type.TypeUtil.getPropertyMapping(CTextView.class, "text");
   AdapterView.OnItemSelectedListener chain;
   AdapterView listView;

   ListSelectListener(AdapterView.OnItemSelectedListener chain, AdapterView listView) {
      this.chain = chain;
      this.listView = listView;
   }

   /**
    * Callback method to be invoked when an item in this view has been
    * selected.
    */
   void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
      if (chain != null)
        chain.onItemSelected(parent, view, position, id);
     
      AndroidUtil.sendDelayedEvents(IListener.VALUE_CHANGED, listView, notifyProps); 
   }

   void onNothingSelected(AdapterView<?> parent) {
      if (chain != null)
        chain.onNothingSelected(parent);

      AndroidUtil.sendDelayedEvents(IListener.VALUE_CHANGED, listView, notifyProps); 
   }
}

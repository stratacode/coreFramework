import sc.bind.IListener;
import android.view.ViewGroup;

class CRadioGroup extends RadioGroup implements RadioGroup.OnCheckedChangeListener {
   static sc.type.IBeanMapper checkedProperty = sc.type.TypeUtil.getPropertyMapping(CRadioGroup.class, "checkedRadioButtonId");
   static sc.type.IBeanMapper selectedIndexProperty = sc.type.TypeUtil.getPropertyMapping(CRadioGroup.class, "selectedIndex");
   {
      super.setOnCheckedChangeListener(this); 
   }

   /** 
    * This is not chainable unfortunately.  You'll have to subclass and override onClick or listen to the clickCount change
    * property.  It is an integer incremented on each click
    */
   public void setOnCheckedChangeListener(View.OnClickListener ocl) {
      throw new IllegalArgumentException("For CRadioGroup, override onCheckedChange method or bind to selectedIndex or selectedId properties");
   }

   private int selectedIndex = -1;

   public void onCheckedChanged(RadioGroup group, int checkedId) { 
      int ct = getChildCount();
      for (int i = 0; i < ct; i++) {
         View child = getChildAt(i);
         if (child.id == checkedId) {
            selectedIndex = i;
            break;
         }
      }
      AndroidUtil.sendDelayedEvent(IListener.VALUE_CHANGED, group, checkedProperty);
      AndroidUtil.sendDelayedEvent(IListener.VALUE_CHANGED, group, selectedIndexProperty);
   }

   int getSelectedIndex() {
      return selectedIndex;
   }
   void setSelectedIndex(int index) {
      selectedIndex = index;
      if (index == -1) 
	 super.clearCheck();
      else if (index < getChildCount()) {
	 View child = getChildAt(index);
	 check(child.id);
      }
   }

   void clearCheck() {
      selectedIndex = -1;
      super.clearCheck();
   }

   public void addView(View child, int index, ViewGroup.LayoutParams params) {
      super.addView(child, index, params);
      if (selectedIndex != -1 && selectedIndex < getChildCount() && getChildAt(selectedIndex) == child)
	 check(child.id);
   }
}

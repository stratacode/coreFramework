import android.text.*;

import sc.bind.IListener;

class CRadioButton extends RadioButton implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
   static sc.type.IBeanMapper clickCountProperty = sc.type.TypeUtil.getPropertyMapping(CRadioButton.class, "clickCount");
   static sc.type.IBeanMapper checkedProperty = sc.type.TypeUtil.getPropertyMapping(CRadioButton.class, "checked");
   {
      super.setOnClickListener(this); 
      super.setOnCheckedChangeListener(this);
   }

   /** 
    * This is not chainable unfortunately.  You'll have to subclass and override onClick or listen to the clickCount change
    * property.  It is an integer incremented on each click
    */
   public void setOnClickListener(View.OnClickListener ocl) {
      throw new IllegalArgumentException("For CRadioButton, override onClick method or bind to clickCount property");
   }

   /** 
    * This is not chainable unfortunately.  You'll have to subclass and override onCheckedChanged or listen to the
    * checked property.
    */
   public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
      throw new IllegalArgumentException("For CRadioButton, override onCheckedChanged method or bind to checked property");
   }

   int clickCount;

   public void setTextString(String str) {
      setText(str);
   }

   public void onClick(View view) {
      // clickCount++; TODO fixme!
      clickCount = clickCount + 1;
   }

   public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
      AndroidUtil.sendDelayedEvent(IListener.VALUE_CHANGED, buttonView, checkedProperty);   
   }

   int getTextColor() {
      return super.getCurrentTextColor();
   }

   void setTextColor(int color) {
      super.setTextColor(color);
   }

}

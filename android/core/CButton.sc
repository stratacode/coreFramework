import android.text.*;

class CButton extends Button implements View.OnClickListener {
   static sc.type.IBeanMapper clickCountProperty = sc.type.TypeUtil.getPropertyMapping(CButton.class, "clickCount");
   {
      super.setOnClickListener(this); 
   }

   /** 
    * This is not chainable unfortunately.  You'll have to subclass and override onClick or listen to the clickCount change
    * property.  It is an integer incremented on each click
    */
   public void setOnClickListener(View.OnClickListener ocl) {
      throw new IllegalArgumentException("For CButton, override onClick method or bind to clickCount property");
   }

   int clickCount;

   public void setTextString(String str) {
      setText(str);
   }

   public void onClick(View view) {
      clickCount++;
   }

   int getTextColor() {
      return super.getCurrentTextColor();
   }

   void setTextColor(int color) {
      super.setTextColor(color);
   }

   void setVisibility(int vis) {
      super.setVisibility(vis);
      refreshDrawableState();
   }
}

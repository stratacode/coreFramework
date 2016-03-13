import android.text.*;

class CEditText extends EditText {
   static sc.type.IBeanMapper textStringProperty = sc.type.TypeUtil.getPropertyMapping(CEditText.class, "textString");

   {
      addTextChangedListener(new TextViewWatcher(this, textStringProperty)); 
   }

   /** EditText modifies its return in place and does not implement equals properly.  You'll always bind to the textString property but for TextView it is a pass through */
   public String getTextString() {
      return getText().toString();
   }

   public void setTextString(String str) {
      setText(str);
   }

   int getTextColor() {
      return super.getCurrentTextColor();
   }

   void setTextColor(int color) {
      super.setTextColor(color);
   }
}

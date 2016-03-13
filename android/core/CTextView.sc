
class CTextView extends TextView {
   static sc.type.IBeanMapper textStringProperty = sc.type.TypeUtil.getPropertyMapping(CTextView.class, "textString");

   {
      addTextChangedListener(new TextViewWatcher(this, textStringProperty)); 
   }

   String getTextString() {
      CharSequence txt = getText();
      return txt == null ? null : txt.toString();
   }

   void setTextString(String str) {
      setText(str);
   }

   int getTextColor() {
      return super.getCurrentTextColor();
   }

   void setTextColor(int color) {
      super.setTextColor(color);
   }
}

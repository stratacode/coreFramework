import sc.bind.BindSettings;

public class IntConverter {
   @Bindable
   public String error = "";

   @Bindable
   public boolean hasError = false;

   /** 
    * The inverse to intToString, typically called indirectly for
    * reverse bindings.  It takes the integer as an input parameter
    * to return so that it is a really invertible expression.  Otherwise
    * it returns a different integer than was input and bi-directional bindings fire
    * for another iteration in reverse.
    */
   @BindSettings(reverseMethod="intToString", modifyParam=true)
   public int stringToInt(CharSequence value) {
      int newValue;
      if (value == null || value.length() == 0) {
         error = "";
         hasError = false;
         newValue = 0;
      }
      else {
         try {
            error = "";
            hasError = false;
            newValue = Integer.valueOf(value.toString());
         }
         catch (NumberFormatException nfe) {
            error = "Invalid integer: " + value;
            newValue = 0;
            hasError = true;
         }
      }
      return newValue;
   }

   public int stringToIntReverse(CharSequence value, int intValue) {
      int newValue = stringToInt(value);
      String oldStrValue = intToString(intValue);
      // To make this function's reverse behavior more accurate, we take the forward value as a param.
      // If the formatted version of this intValue is equal to what we're given, return
      // the full precision value.  Otherwise, do the conversion.
      if (oldStrValue.equals(value)) {
         error = "";
         return intValue;
      }
      return newValue;
   }

   @BindSettings(reverseMethod="stringToIntReverse", modifyParam=true)
   public String intToString(int intValue) {
      return String.valueOf(intValue);
   }
}

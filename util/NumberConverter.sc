import sc.bind.BindSettings;

public class NumberConverter {
   @Bindable
   public String error = "";

   @Bindable
   public boolean hasError = false;

   /** 
    * The inverse to numberToString, typically called indirectly for
    * reverse bindings.  It takes the number as an input parameter
    * to return so that it is a really invertible expression.  Otherwise
    * it returns a different number than was input and bi-directional bindings fire
    * for another iteration in reverse.
    */
   @BindSettings(reverseMethod="numberToString", modifyParam=true)
   public double stringToNumber(CharSequence value) {
      double newValue;
      if (value == null || value.length() == 0) {
         error = "";
         hasError = false;
         newValue = 0.0;
      }
      else {
         try {
            error = "";
            hasError = false;
            newValue = Double.valueOf(value.toString());
         }
         catch (NumberFormatException nfe) {
            error = "Invalid number: " + value;
            newValue = 0.0;
            hasError = true;
         }
      }
      return newValue;
   }

   public double stringToNumberReverse(CharSequence value, double number) {
      double newValue = stringToNumber(value);
      String oldStrValue = numberToString(number);
      // To make this function's reverse behavior more accurate, we take the forward value as a param.
      // If the formatted version of this number is equal to what we're given, return
      // the full precision value.  Otherwise, do the conversion.
      if (oldStrValue.equals(value)) {
         error = "";
         return number;
      }
      return newValue;
   }

   String format = "#.##";

   @BindSettings(reverseMethod="stringToNumberReverse", modifyParam=true)
   public String numberToString(double number) {
      return TextUtil.format(format, number);
      //return String.format("%.2g%n", number);
   }
}

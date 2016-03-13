import java.text.DecimalFormat;

@sc.js.JSSettings(jsLibFiles="js/util.js", prefixAlias="sc_")
public class TextUtil {
   public static String format(String format, double number) {
      DecimalFormat df = new DecimalFormat(format);
      return df.format(number);
   }

   // Currently the data binding system can't listen on method calls but can for function arguments so this is a workaround
   public static int length(String str) {
      if (str == null)
         return 0;
      return str.length();
   }

   public static String escapeHTML(String in) {
      return StringUtil.escapeHTML(in, false).toString();
   }

   public static String escapeQuotes(String in) {
      return in.replace("'","&apos;").replace("\"", "&quot;");
   }
}

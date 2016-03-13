function sc_TextUtil() {
}

var sc_TextUtil_c = sc_newClass("sc_TextUtil", sc_TextUtil, null, null);

sc_TextUtil_c.format = function(format, number) {
   var prec = format.length;
   var ix = format.indexOf(".");
   if (ix != -1)
      prec = prec - ix - 1;
   var res = number.toFixed(prec);
   var cull = 0;
   // The JS toFixed leaves on the trailing 0's but we need this to match the Java version
   for (var i = res.length-1; i >= 1; i--) {
      var c = res.charAt(i);
      if (c == '0')
         cull++;
      else if (c == '.') {
         cull++;
         break;
      }
      else
         break;
   }
   if (cull > 0)
      res = res.substring(0, res.length - cull);
   return res;
}

sc_TextUtil_c.length = function(str) {
   if (str == null)
      return 0;
   return str.length;
}

sc_TextUtil_c.escapeHTML = function(input) {
    return input
         .replace(/&/g, "&amp;")
         .replace(/</g, "&lt;")
         .replace(/>/g, "&gt;")
         .replace(/"/g, "&quot;")
         .replace(/'/g, "&#039;");
}

sc_TextUtil_c.escapeQuotes = function(input) {
    return input
         .replace(/"/g, "&quot;")
         .replace(/'/g, "&#039;");
}

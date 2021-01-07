function sc_TextUtil() {
}

var sc_TextUtil_c = sc_newClass("sc_TextUtil", sc_TextUtil, null, null);

sc_TextUtil_c.format = function(format, number) {
   if (!number) {
      return "0"; // In the server version number is a primitive so is never null
   }
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
   if (str === null)
      return 0;
   return str.length;
}

sc_TextUtil_c.isEmpty = function(str) {
   return str === null || str.length === 0;
}

sc_TextUtil_c.equals = function(s1, s2) {
   return s1 == s2;
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

sc_TextUtil_c.hasMapEntry = function(m,k) {
   return m !== null && m.get(k) !== null;
}

sc_TextUtil_c.hourMillis = 1000 * 60 * 60;
sc_TextUtil_c.dayMillis = sc_TextUtil_c.hourMillis * 24;

sc_TextUtil_c.formatUserDate = function(date, includeTime) {
   if (date == null)
      return "";
   var now = new Date();
   var sb = new jv_StringBuilder();
   var nowTime = now.getTime();
   var dateTime = date.getTime();
   var handled = false;
   var delta = nowTime - dateTime;
   if (delta > 0 || -delta < sc_TextUtil_c.hourMillis) {
      if (delta < sc_TextUtil_c.dayMillis) {
         if (now.getDate() == date.getDate())
            sb.append("today");
         else if (new Date(dateTime + sc_TextUtil_c.dayMillis).getDate() == now.getDate())
            sb.append("yesterday");
         handled = true;
      }
      else if (new Date(dateTime + sc_TextUtil_c.dayMillis).getDate() == now.getDate()) {
         sb.append("yesterday");
         handled = true;
      }
   }
   else if (delta < 0 && -delta < sc_TextUtil_c.dayMillis && new Date(nowTime + sc_TextUtil_c.dayMillis).getDate() == date.getDate()) {
      sb.append("tomorrow");
      handled = true;
   }

   if (!handled) {
      sb.append(date.getYear() + 1900);
      sb.append("-");
      sb.append(sc_TextUtil_c.twoDigit(date.getMonth()+1));
      sb.append("-");
      sb.append(sc_TextUtil_c.twoDigit(date.getDate()));
   }
   if (includeTime) {
      sb.append(" ");
      sb.append(date.getHours());
      sb.append(":");
      sb.append(sc_TextUtil_c.twoDigit(date.getMinutes()));
   }
   return sb.toString();
}

sc_TextUtil_c.twoDigit = function(val) {
   if (val < 10)
      return "0" + val;
   return val.toString();
}

sc_TextUtil_c.formatDuration = function(val) {
   var sb = new jv_StringBuilder();
   var numDays = millis / dayMillis;
   millis = millis - numDays * dayMillis;
   var numHours = millis / hourMillis;
   millis = millis - numHours * hourMillis;
   var numMinutes = millis / (1000 * 60);
   millis = millis - numMinutes * 1000 * 60;
   var numSecs = millis / 1000;
   millis = millis - numSecs * 1000;
   var first = true;
   if (numDays > 0) {
      sb.append(numDays + "d");
      first = false;
   }
   if (numHours > 0 || !first) {
      sb.append(numHours + "h");
      first = false;
   }
   if (numMinutes > 0 || !first) {
      sb.append(twoDigit(numMinutes) + "m");
   }
   if (numSecs > 0 || !first) {
      sb.append(twoDigit(numSecs));
      if (millis > 0) {
         sb.append("." + millis);
      }
   }
   return sb.toString();
}

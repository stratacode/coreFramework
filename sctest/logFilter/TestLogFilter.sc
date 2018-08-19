
import sc.lang.pattern.Pattern;
import sc.lang.PatternLanguage;
import sc.lang.SCLanguage;
import sc.parser.Parselet;
import sc.parser.ParseError;
import sc.parser.Language;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Used by the test scripts to eliminate or filter lines in the log file before we check the output
 * For example, if there's a date, build number or error message which has an object id you can choose to
 * either ignore it, or match it by an algorithm, or replace the variable parts with something consistent - i.e. a date with the string 'date'
 */
public class TestLogFilter {
   public String[] excludePatterns = {
                                // chrome error messages - weird errors show up in headless chrome so ignoring - turns out this is probably only needed for the chrome std-out log which hasn't been that helpful so far
                                "\\[{digits}/{digits}.{digits}:ERROR:{escapedString}",
                                // scc build stamps for when you run -v
                                "{whiteSpace}scc version: v{digits}.{digits}.{digits}-{identifier}.b{digits}{whiteSpace}@{whiteSpace}{escapedString}",
                                "{whiteSpace}/Applications/Google Chrome.app/Contents/Versions/{digits}.{digits}.{digits}.{digits}/Google Chrome Framework.framework/Versions/Current/Libraries/libswiftshader_libGLESv2.dylib: stat() failed with errno=1{whiteSpace}"
                                    };

   public Parselet[] parseletList;

   public Language language = SCLanguage.getSCLanguage();

   public void init() {
      parseletList = new Parselet[excludePatterns.length];

      for (int i = 0; i < excludePatterns.length; i++) {
         Object patObj = Pattern.initPatternParselet(language, null, excludePatterns[i]);
         if (patObj instanceof ParseError) {
            System.err.println("*** TestLogFilter - invalid pattern: " + excludePatterns[i] + ": "+ patObj);
         }
         else {
            parseletList[i] = (Parselet) patObj;
         }
      }
   }

   public void doFilter(InputStream in, PrintStream out) {
      try {
         BufferedReader bufIn = new BufferedReader(new InputStreamReader(in));
         for (String nextLine = bufIn.readLine(); nextLine != null; nextLine = bufIn.readLine()) {
            boolean excluded = false;
            for (Parselet excludePattern:parseletList) {
               if (language.matchString(nextLine, excludePattern)) {
                  excluded = true;
                  break;
               }
            }
            if (!excluded)
               out.println(nextLine);
         }
      }
      catch (IOException exc) {
         System.err.println("*** TestLogFilter - IOException: " + exc);
      }
   }

   @sc.obj.MainSettings
   public static void main(String[] args) {
      TestLogFilter filter = new TestLogFilter();
      filter.init();
      filter.doFilter(System.in, System.out);
   }
}


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
 * Used by the test scripts to eliminate or filter lines in the log file before we do comparisons
 */
public class TestLogFilter {
   public String[] excludePatterns = {"\\[{integerLiteral}/{integerLiteral}.{integerLiteral}:ERROR:{escapedString}"};

   public Parselet[] parseletList;

   public Language language = SCLanguage.getSCLanguage();

   public void init() {
      parseletList = new Parselet[excludePatterns.length];

      for (int i = 0; i < excludePatterns.length; i++) {
         Object patObj = Pattern.initPattern(language, null, excludePatterns[i]);
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
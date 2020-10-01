
import sc.lang.pattern.Pattern;
import sc.lang.pattern.ReplaceResult;
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

import java.util.ArrayList;
import sc.util.ComponentList;

/**
 * Used by the test scripts to eliminate or filter lines in the log file before we check the output
 * For example, if there's a date, build number or error message which has an object id you can choose to
 * either ignore it, or match it by an algorithm, or replace the variable parts with something consistent - i.e. a date with the string 'date'
 */
public class TestLogFilter {
   // List of globally applied patterns written using SCLanguage parselets to parse specific chunks of the matched string. If the string matches,
   // it is discarded from the output.
   public String[] globalExcludePatterns = {
                                // chrome error messages - weird errors show up in headless chrome so ignoring - turns out this is probably only needed for the chrome std-out log which hasn't been that helpful so far
                                "\\[{digits}/{digits}.{digits}:ERROR:{escapedString}",
                                // scc build stamps for when you run -v
                                "{whiteSpace}scc version: v{digits}.{digits}.{digits}[-{identifier}][_{identifier}].b{digits}{whiteSpace}@{whiteSpace}{escapedString}",
                                "{whiteSpace}/Applications/Google Chrome.app/Contents/Versions/{digits}.{digits}.{digits}.{digits}/Google Chrome Framework.framework/Versions/Current/Libraries/libswiftshader_libGLESv2.dylib: stat\\(\\) failed with errno=1{whiteSpace}",
                                // the scc jar which expanded to a specific version and build
                                // These next four are for errors emited by jogl on the mac - warning/exception due to some method called from the wrong thread
                                "{whiteSpace}{digits}-{digits}-{digits}{whiteSpace}{digits}:{digits}:{digits}.{digits}{whiteSpace}java{escapedString}",
                                "{whiteSpace}{digits}{whiteSpace}AppKit{escapedString}",
                                "{whiteSpace}{digits}{whiteSpace}libnative{escapedString}",
                                "{whiteSpace}{digits}{whiteSpace}???{escapedString}"
                                    };

   // List of globally applied patterns which are filtered. The output line will look like the input line except that
   // named variables are replaced with the variable name. e.g. replacing an id with "digits" or a date with {date}.
  public String[] globalReplacePatterns = {
    "{whiteSpace}\"sessionMarker\":\"{marker=escapedString}\""
  };

   public ArrayList<Pattern> excludePatterns;
   public ArrayList<Pattern> replacePatterns;

   // List of custom log filters you specify via the logFilterOpts file in the test/valid/testName/logFilterOpts file
   // TODO: add logFilterLayers - so we can keep the filter code next to the code that adds the log filters
   object options extends ComponentList<FilterOption> {
      object replaceWindowId extends FilterOption {
         optionName = "sc_windowId";
         replace = true; // We'll replace var sc_windowId = 101 with var_sc_windowId = {windowId} for apps where it might vary
         patternStrings = {"{whiteSpace}var{whiteSpace}sc_windowId{whiteSpace}={whiteSpace}{windowId=digits};{whiteSpace}"};
      }

      object webIdFilter extends FilterOption {
         optionName = "webIds";
         replace = true;
         patternStrings = {
                "id={quoteChar}{alphaNumString}", 
                "_{id=digits}",
                "for={quoteChar}{alphaNumString}",
                "{alphaNumString}__{id=digits}"
         };
      }

      object dbIdFilter extends FilterOption {
         optionName = "dbIds";
         replace = true;
         patternStrings = {
                "id = {id=digits}",
                "{alphaNumString}__{id=digits}",
                // format of id references in JSON properties
                "ref:db:{id=digits}"
         };
      }

      object longLinesFilter extends FilterOption {
         optionName = "longLines";
         maxLineLength = 256;
      }
   }

   public Language language = SCLanguage.getSCLanguage();

   public class FilterOption {
      String optionName;
      boolean replace = false;
      int maxLineLength = -1;
      String[] patternStrings;
   }

   int maxLineLength = -1;

   public void init(String[] args) {
      excludePatterns = new ArrayList<Pattern>(globalExcludePatterns.length);
      appendPatternList(excludePatterns, globalExcludePatterns);

      replacePatterns = new ArrayList<Pattern>(globalReplacePatterns.length);
      appendPatternList(replacePatterns, globalReplacePatterns);

      for (String arg:args) {
         boolean found = false;
         for (FilterOption opt:options) {
            if (arg.startsWith("-")) {
               if (opt.optionName.equals(arg.substring(1))) {
                  found = true;
                  if (opt.maxLineLength != -1)
                     maxLineLength = opt.maxLineLength;
                  if (opt.patternStrings != null) {
                     ArrayList<Pattern> patternList = opt.replace ? replacePatterns : excludePatterns;
                     appendPatternList(patternList, opt.patternStrings);
                  }
               }
            }
         }
         if (!found)
            System.err.println("*** Invalid option to logFilter: " + arg + " should be one of: " + getOptionUsage());
      }
   }

   String getOptionUsage() {
      StringBuilder sb = new StringBuilder();
      for (FilterOption opt:options) {
         if (sb.length() > 0)
            sb.append(" ");
         sb.append("-");
         sb.append(opt.optionName);
      }
      return sb.toString();
   }


   public void appendPatternList(ArrayList<Pattern> patternList, String[] patternStrings) {
      for (int i = 0; i < patternStrings.length; i++) {
         Object patObj = Pattern.initPattern(language, null, patternStrings[i]);
         if (patObj instanceof ParseError) {
            System.err.println("*** TestLogFilter - invalid replacePattern: " + patternStrings[i] + ": "+ patObj);
         }
         else {
            patternList.add((Pattern) patObj);
         }
      }
   }

   public void doFilter(InputStream in, PrintStream out) {
      try {
         BufferedReader bufIn = new BufferedReader(new InputStreamReader(in));
         for (String nextLine = bufIn.readLine(); nextLine != null; nextLine = bufIn.readLine()) {
            boolean excluded = false;
            if (maxLineLength != -1 && nextLine.length() > maxLineLength) {
               excluded = true;
               out.println("<excluded-long-line>");
            }
            if (!excluded) {
               for (Pattern excludePattern:excludePatterns) {
                  if (excludePattern.matchSimpleString(nextLine)) {
                     excluded = true;
                     break;
                  }
               }
            }
            if (!excluded) {
               for (Pattern replacePattern:replacePatterns) {
                  String replaceRes = replacePattern.replaceString(nextLine);
                  if (replaceRes != null)
                     nextLine = replaceRes;
               }
               out.println(nextLine);
            }
         }
      }
      catch (IOException exc) {
         System.err.println("*** TestLogFilter - IOException: " + exc);
      }
   }

   @sc.obj.MainSettings
   public static void main(String[] args) {
      TestLogFilter filter = new TestLogFilter();
      filter.init(args);
      filter.doFilter(System.in, System.out);
   }
}

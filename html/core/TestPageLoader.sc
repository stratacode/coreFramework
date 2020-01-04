import sc.js.URLPath;
import java.util.List;
import java.util.ArrayList;
import sc.util.FileUtil;
import sc.type.PTypeUtil;
import sc.obj.CurrentScopeContext;
import sc.obj.ScopeContext;
import sc.obj.AppGlobalScopeDefinition;
import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import sc.dyn.DynUtil;
import sc.lang.AbstractInterpreter;
import sc.layer.LayeredSystem;

import sc.lang.html.QueryParamProperty;

import sc.layer.AsyncProcessHandle;

// Using runtimes='default' here so this code is run for the 'java server' runtime only in client/server mode and run on the server for the 'js only' case,
// Use default in general when there's a need to run code in the "bootstrap runtime" - i.e. the one generating the code for the other runtime.   This way 
// we can still launch and control a browser session that talks to the JS-only application.  
// With default, this class will not be included in the JS runtime for client/server mode - nor can it be run there because some of the dependencies 
// require it to be in the Java runtime (like the ability to launch a web browser).
@sc.obj.Exec(runtimes="default")
// This keeps the class from being included in the actual Javascript since it has dependencies which don't exist there
@sc.js.JSSettings(jsLibFiles="js/tags.js")
public class TestPageLoader implements sc.obj.ISystemExitListener {
   AbstractInterpreter cmd;
   LayeredSystem sys; 
   List<URLPath> urlPaths; 

   public int waitForPageTime = 190000;
   public int waitForRuntimeTime = 190000;

   public boolean loadAllPages = true;
   public boolean recordClientOutput = true;

   Map<String,Integer> savePageIndex = new TreeMap<String,Integer>();

   // Processes started by the test page loader for browser instances
   List<AsyncProcessHandle> processes = new ArrayList<AsyncProcessHandle>();

   // If true, open the chrome with --headless
   public boolean headless;
   /**
    * When clientSync is true, we use RPC to talk to the browser process
    * as part of testing - to fetch page contents, and logs. This is the case for
    * serverTags or client/server mode when sync is enabled.
    * When it's false, we get the page output from chrome command using a command line option but
    * currently do not run page tests.
    */
   public boolean clientSync;

   public boolean skipIndexPage = true;

   String getPlatformChromeCommand() {
      String osName = System.getProperty("os.name");
      if (osName == null || osName.contains("Mac OS X"))
         return "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
      else if (osName.contains("Windows"))
         return "chrome";
      else // Assuming linux
         return "/usr/bin/google-chrome";
   }

   String chromeCmd = getPlatformChromeCommand();
   String openCmd = sc.type.PTypeUtil.getPlatformOpenCommand();

   public TestPageLoader(AbstractInterpreter cmd) {
      this.cmd = cmd;
      this.sys = cmd.system;
      this.urlPaths = sys.buildInfo.getURLPaths();
      this.headless = sys.options.headless;

      // Disable these timeouts when we are debugging the program
      if (sys.options.testDebugMode) {
         // Nice to keep debug sessions open for a long time without timing out :)
         waitForRuntimeTime = waitForPageTime = 60*60*24*3*1000;
      }

      System.out.println("--- Waiting for server to start...");
      sys.addSystemExitListener(this);
      //cmd.sleep(5000);
      if (sys.serverEnabled && !sys.waitForRuntime(waitForRuntimeTime))
         throw new IllegalArgumentException("Server failed to start in 5 seconds");
      System.out.println("- Server started");

      LayeredSystem jsPeer = sys.getPeerLayeredSystem("js");
      // TODO: maybe we should enable server tag sync using a new html.sync
      // layer and that would simplify this logic and add the ability to
      // use server tags without sync.
      clientSync = sys.serverEnabled && (jsPeer == null || sys.syncEnabled);
      System.out.println("TestPageLoader initialized with clientSync: " + clientSync + " headless: " + headless + " for urls: " + urlPaths);
   }

   AsyncProcessHandle openBrowser(String url, String pageResultsFile) {
      AsyncProcessHandle processRes = null;
      if (headless) {
         if (clientSync) {
            System.out.println("Opening headless sync url: " + url);

            // To debug problems that only show up in headless mode, add --remote-debugging-port=9222 and then navigate to localhost:9222 in another browser instance. You'll see
            // see a thumbnail window of the output and the console errors there in the attached session.  Also --enable-logging --v=1 might be helpful for chrome's internal log.
            // TODO: to access the js console from headless without using 'sync' (i.e. if there's an RTE initializing the app) we should use the devtools protocol
            processRes = cmd.execAsync('"' + chromeCmd + '"' + " --remote-debugging-port=9222 --headless --auto-open-devtools-for-tabs --disable-gpu --user-profile=/tmp/chrome-test-profile-dir " + url + " > /tmp/chromeHeadless.out");
         }
         // client only hopefully we can just rely on chrome to save the dom with --dump-dom
         else {
            System.out.println("Opening headless client-only url: " + url);
            new File(pageResultsFile).getParentFile().mkdirs();
            processRes = cmd.execAsync('"' + chromeCmd + '"' + " --headless --disable-gpu --dump-dom --user-profile=/tmp/chrome-test-profile-dir " + url + " > " + pageResultsFile);
            cmd.sleep(1000);
            //System.out.println("*** chrome saved: " + pageResultsFile + " size: " + FileUtil.getFileAsString(pageResultsFile).length());
         }
         if (processRes != null)
            processes.add(processRes);
      }
      else {
         System.out.println("Opening browser with: " + url);

         if (openCmd.equals("open"))
            cmd.exec(openCmd + " " + url);
         else
            cmd.execAsync(openCmd + " " + url);
         cmd.sleep(2000); // give user time for opening devtools before starting any script
      }
      return processRes;
   }

   public URLResult loadPage(String name, String scopeContextName) {
      boolean found = false;
      AsyncProcessHandle res = null;
      URLPath urlPath = findUrlPath(name);
      if (urlPath != null)
         return loadURL(urlPath, scopeContextName);
      else
         throw new IllegalArgumentException("TestPageLoader.loadPage - " + name + " not found");
   }

   public CurrentScopeContext loadPageAndWait(String pageName, String scopeContextName) {
       loadPage(pageName, scopeContextName);
       CurrentScopeContext ctx = cmd.waitForReady(scopeContextName, waitForPageTime);
       if (ctx == null)
          throw new AssertionError("TestPageLoader.loadPageAndWait(" + pageName + ", " + scopeContextName + ") - timed out waiting for connect");
       return ctx;
   }

   public URLPath findUrlPath(String name) {
      boolean found = false;
      for (URLPath urlPath:urlPaths) {
         if (urlPath.name.equals(name)) {
            return urlPath;
         }
      }
      return null;
   }

   public void savePage(String name, String pageContents) {
      URLPath urlPath = findUrlPath(name);
      if (urlPath != null) {
         Integer ix = savePageIndex.get(name);
         if (ix == null)
            ix = 1;
         saveURL(urlPath, getPageResultsFile(urlPath, "." + ix), pageContents);
         savePageIndex.put(name, ix+1);
      }
      else
         throw new IllegalArgumentException("TestPageLoader.savePage - " + name + " not found");
   }

   String getPageResultsFile(URLPath urlPath, String suffix) {
      return FileUtil.concat(sys.options.testResultsDir, "pages", urlPath.name + suffix);
   }

   static class URLResult {
      AsyncProcessHandle processHandle;
      URLResult(AsyncProcessHandle ph) {
         processHandle = ph;
      }
   }

   public URLResult loadURL(URLPath urlPath, String scopeContextName) {
      String pageResultsFile = getPageResultsFile(urlPath, "");
      System.out.println("loadURL: " + urlPath.name + " at: " + urlPath.url);

      // Returns file:// or http:// depending on whether the server is enabled.  Also finds the files in the first buildDir where it exists
      String url = sys.getURLForPath(urlPath);
      if (url == null)
         return null;

      if (scopeContextName != null) {
         url = URLPath.addQueryParam(url, "scopeContextName", scopeContextName);
      }

      AsyncProcessHandle processRes = openBrowser(url, pageResultsFile);

      try {
         if (!sys.serverEnabled || scopeContextName == null) {
            System.out.println("--- Waiting for client to connect...");
            cmd.sleep(1500);
            System.out.println("- Done waiting for client to connect");
         }

         if (clientSync) {
            PTypeUtil.setAppId(urlPath.keyName);

            if (scopeContextName != null) {
               if (sc.obj.CurrentScopeContext.waitForReady(scopeContextName, waitForPageTime) == null) {
                  endSession(processRes);
                  processRes = null;
                  throw new IllegalArgumentException("Timeout opening url: " + url + " after: " + waitForPageTime + " millis - no client request for scope context: " + scopeContextName); 
               }
            }
            // for the initial page load, we just use the innerHTML which seems accurate and represents the rendered content from the initial page load
            saveURL(urlPath, pageResultsFile, getClientBodyHTML(scopeContextName));
         }
      }
      catch (RuntimeException exc) {
         endSession(processRes);
         processRes = null;
         System.err.println("*** Exception from loadURL: " + urlPath.url + ": " + exc);
         exc.printStackTrace();
         throw exc;
      }
      return new URLResult(processRes);
   }

   public void endSession(AsyncProcessHandle processRes) {
      if (processRes != null) {
         //System.out.println("Ending browser process");
         processRes.endProcess();
      }
   }

   // NOTE: using this for tests is not very robust when using the JS runtime 
   // because any changes made to the elements of the DOM are not reflected.  But
   // for server tags, it works because we update both.  For the JS runtime
   // we use the tag objects to generate the HTML output that reflects the 
   // page's current state - i.e. the output_c() method. 
   public String getClientBodyHTML(String scopeContextName) {
      ScopeContext scopeContext = scopeContextName == null ? AppGlobalScopeDefinition.getAppGlobalScope() : CurrentScopeContext.get(scopeContextName).getScopeContextByName("window");

      return (String) DynUtil.evalRemoteScript(scopeContext, "document.body.innerHTML;");
   }

   public String getClientConsoleLog(String scopeContextName) {
      ScopeContext scopeContext = scopeContextName == null ? AppGlobalScopeDefinition.getAppGlobalScope() : CurrentScopeContext.get(scopeContextName).getScopeContextByName("window");
      return (String) DynUtil.evalRemoteScript(scopeContext, "sc_getConsoleLog();");
   }

   void saveURL(URLPath urlPath, String pageResultsFile, String pageContents) {
      System.out.println("Getting DOM: " + urlPath.name);
      if (pageContents == null)
         System.err.println("*** No page contents for: " + urlPath + " to store in file: " + pageResultsFile);
      else {
         // Set the app-id so we restrict the contexts we search to just this application - theoretically, we could iterate over the sessions here too to target a specific browser instance to make it more robust
         FileUtil.saveStringAsFile(pageResultsFile, pageContents, true);
         System.out.println("- DOM results: " + urlPath.name + " length: " + pageContents.length() + (sys.options.testVerifyMode ? "" : " path: " + pageResultsFile));
      }
   }

   boolean runPageTest(URLPath urlPath, String scopeContextName) {
      String typeName = sc.type.CTypeUtil.capitalizePropertyName(urlPath.name);
      String testScriptName = "test" + typeName + ".scr";
      if (cmd.exists(testScriptName)) {
         if (!clientSync) // TODO: in this case, we'd like to convert the testApp.scr file into a program to download when we run in testMode
            System.out.println("Skipping " + testScriptName + " for type: " + typeName + " - scripts not yet supported for client-only application");
         else {
            System.out.println("--- Running " + testScriptName + " for type: " + typeName);
            String saveCtxName = null;
            if (scopeContextName != null) {
               saveCtxName = cmd.scopeContextName;
               cmd.scopeContextName = scopeContextName;
               cmd.targetScopeName = "window";
            }
            cmd.include(testScriptName);
            if (scopeContextName != null) {
               saveCtxName = saveCtxName;
               cmd.scopeContextName = scopeContextName;
               cmd.targetScopeName = null;
            }

            System.out.println("- Done: " + testScriptName);
            return true;
         }
     }
     return false;
   }

   public void loadAllPages() {
      int numLoaded = 0;
      System.out.println("--- Loading all pages from: " + urlPaths.size() + " urls");
      boolean indexSkipped = false;
      for (URLPath urlPath:urlPaths) {
         // Simple applications have only a single URL - the root.  Others have an index page and the application pages so we only skip when there's more than one
         if (skipIndexPage && urlPath.name.equals("index") && urlPaths.size() > 1) {
            indexSkipped = true;
            continue;
         }

         if (!sys.testPatternMatches(urlPath.name))
            continue;

         Object pageType = urlPath.pageType;
         List<QueryParamProperty> queryParams = pageType == null ? null : QueryParamProperty.getQueryParamProperties(pageType);
         if (queryParams != null) {
            boolean skip = false;
            for (QueryParamProperty queryParam:queryParams) {
               if (queryParam.required) {
                  System.out.println("*** Skipping url: " + urlPath + " for required query param: " + queryParam.paramName);
                  skip = true;
                  break;
               }
            }
            if (skip)
               continue;
         }

         // If we are syncing to the client, use the unique id for this URL to choose the name of the
         // 'scope context' (essentially the id of the browser window so we can target the savePage and
         // saveClientConsole methods at the right window).
         String scopeContextName = clientSync ? urlPath.keyName : null;
         URLResult processRes = loadURL(urlPath, scopeContextName);
         if (processRes == null) {
            System.out.println("Skipping test for incomplete url: " + urlPath);
            continue;
         }

         try {
            boolean ranPageTest = runPageTest(urlPath, scopeContextName);

            saveClientConsole(urlPath, scopeContextName);

            // Save the page after it's run a page test. Otherwise, it's the same thing and not worth the effort
            if (clientSync && ranPageTest)
               savePage(urlPath.name, getClientBodyHTML(scopeContextName));
         }
         finally {
            endSession(processRes.processHandle);
         }
         numLoaded++;
      }
      System.out.println("- Done loading: " + numLoaded + " pages" + (indexSkipped ? " - skipIndexPage set" : ""));
   }

   public void saveClientConsole(URLPath urlPath, String scopeContextName) {
      if (clientSync && recordClientOutput) {
         String consoleLog = getClientConsoleLog(scopeContextName);
         String consoleResultsFile = getPageResultsFile(urlPath, ".jsConsole");
         FileUtil.saveStringAsFile(consoleResultsFile, consoleLog, true);
      }

   }

   public void systemExiting() {
      for (AsyncProcessHandle process:processes) {
         //System.out.println("System exiting - ending process");
         process.endProcess();
      }
      processes.clear();
   }
}

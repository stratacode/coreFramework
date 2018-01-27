import sc.js.URLPath;
import java.util.List;
import java.util.ArrayList;
import sc.util.FileUtil;
import sc.obj.ScopeEnvironment;
import sc.obj.CurrentScopeContext;
import sc.obj.AppGlobalScopeDefinition;
import java.io.File;
import sc.dyn.DynUtil;
import sc.lang.AbstractInterpreter;

import sc.layer.AsyncProcessHandle;

// Need this to run for the 'java server' only in client/server mode but also 'js only' case.
@sc.obj.Exec(runtimes="default")
public class TestPageLoader implements sc.obj.ISystemExitListener {
   AbstractInterpreter cmd;
   sc.layer.LayeredSystem sys; 
   List<URLPath> urlPaths; 

   public int waitForPageTime = 5000;
   public int waitForRuntimeTime = 5000;

   public boolean loadAllPages = true;
   public boolean recordClientOutput = true;

   // Holds any started processes
   List<AsyncProcessHandle> processes = new ArrayList<AsyncProcessHandle>();

   public boolean headless;
   public boolean clientSync;

   public boolean skipIndexPage = true;

   String chromeCmd = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

   public TestPageLoader(AbstractInterpreter cmd) {
      this.cmd = cmd;
      this.sys = cmd.system;
      this.urlPaths = sys.getURLPaths();
      this.headless = sys.options.headless;

      System.out.println("Waiting for server to start...");
      sys.addSystemExitListener(this);
      //cmd.sleep(5000);
      if (sys.serverEnabled && !sys.waitForRuntime(waitForRuntimeTime))
         throw new IllegalArgumentException("Server failed to start in 5 seconds");

      // To do testing via sync we need the server and the JS runtime at least. 
      clientSync = sys.serverEnabled && sys.getPeerLayeredSystem("js") != null;
   }

   AsyncProcessHandle openBrowser(String url, String pageResultsFile) {
      AsyncProcessHandle processRes = null;
      if (headless) {
         if (clientSync) {
            System.out.println("Opening headless sync url: " + url);

            processRes = cmd.execAsync('"' + chromeCmd + '"' + " --headless --auto-open-devtools-for-tabs --disable-gpu --repl --user-profile=/tmp/chrome-test-profile-dir " + url + " > /tmp/chromeHeadless.out");
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
         cmd.exec("open " + url);
         cmd.sleep(2000); // give user time for opening devtools before starting any script
      }
      return processRes;
   }

   public AsyncProcessHandle loadPage(String name, String scopeContextName) {
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

   public void savePage(String name, int ix, String pageContents) {
      URLPath urlPath = findUrlPath(name);
      if (urlPath != null) {
         saveURL(urlPath, getPageResultsFile(urlPath, "." + ix), pageContents);
      }
      else
         throw new IllegalArgumentException("TestPageLoader.savePage - " + name + " not found");
   }

   String getPageResultsFile(URLPath urlPath, String suffix) {
      return FileUtil.concat(sys.options.testResultsDir, "pages", urlPath.name + suffix);
   }

   public AsyncProcessHandle loadURL(URLPath urlPath, String scopeContextName) {
      String pageResultsFile = getPageResultsFile(urlPath, "");
      System.out.println("loadURL: " + urlPath.name + " at: " + urlPath.url);

      // Returns file:// or http:// depending on whether the server is enabled.  Also finds the files in the first buildDir where it exists
      String url = sys.getURLForPath(urlPath.cleanURL(!sys.serverEnabled));

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
            ScopeEnvironment.setAppId(URLPath.getAppNameFromURL(urlPath.url));

            if (scopeContextName != null) {
               if (sc.obj.CurrentScopeContext.waitForReady(scopeContextName, waitForPageTime) == null) {
                  endSession(processRes);
                  throw new IllegalArgumentException("Timeout opening url: " + url + " - client never requested scope context: " + scopeContextName); 
               }
            }
            // for the inital page load, we just use the innerHTML which seems accurate and represents the rendered content from the initial page load
            saveURL(urlPath, pageResultsFile, getClientBodyHTML());
         }
      }
      catch (RuntimeException exc) {
         endSession(processRes);
         processRes = null;
         System.err.println("*** Caught exception in loadURL: " + urlPath.url + ": " + exc);
         exc.printStackTrace();
         throw exc;
      }
      return processRes;
   }

   public void endSession(AsyncProcessHandle processRes) {
      if (processRes != null) {
         //System.out.println("Ending browser process");
         processRes.endProcess();
      }
   }

   // NOTE: using this for tests is not very robust because any changes made to the elements of the DOM are not reflected.  Instead, we'll
   // use the tag objects to generate the HTML output that reflects the page's current state. 
   public String getClientBodyHTML() {
      return (String) DynUtil.evalRemoteScript(AppGlobalScopeDefinition.getAppGlobalScope(), "document.body.innerHTML;");
   }

   public String getClientConsoleLog() {
      return (String) DynUtil.evalRemoteScript(AppGlobalScopeDefinition.getAppGlobalScope(), "sc_getConsoleLog();");
   }

   void saveURL(URLPath urlPath, String pageResultsFile, String pageContents) {
      System.out.println("Getting DOM: " + urlPath.name);
      // Set the app-id so we restrict the contexts we search to just this application - theoretically, we could iterate over the sessions here too to target a specific browser instance to make it more robust
      FileUtil.saveStringAsFile(pageResultsFile, pageContents, true);
      System.out.println("- DOM results: " + urlPath.name + " length: " + pageContents.length() + (sys.options.testVerifyMode ? "" : " path: " + pageResultsFile));
   }

   void runPageTest(URLPath urlPath) {
      String testScriptName = "test" + sc.type.CTypeUtil.capitalizePropertyName(urlPath.name) + ".scr";
      if (cmd.exists(testScriptName)) {
         if (!clientSync) // Ideally in this case, we'd have a way to convert the testApp.scr file into a program to download when we run in testMode
             System.out.println("Skipping testScript: " + testScriptName + " for client-only application");
         else
             cmd.include(testScriptName);
      }
   }

   public void loadAllPages() {
      int numLoaded = 0;
      for (URLPath urlPath:urlPaths) {
         // Simple applications have only a single URL - the root.  Others have an index page and the application pages so we only skip when there's more than one
         if (skipIndexPage && urlPath.name.equals("index") && urlPaths.size() > 1)
            continue;
         AsyncProcessHandle processRes = loadURL(urlPath, null);

         runPageTest(urlPath);

         saveClientConsole(urlPath);

         endSession(processRes);
         numLoaded++;
      }
      System.out.println("Done loading: " + numLoaded + " pages...");
   }

   public void saveClientConsole(URLPath urlPath) {
      if (clientSync && recordClientOutput) {
         String consoleLog = getClientConsoleLog();
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

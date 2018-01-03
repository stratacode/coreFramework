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

public class TestPageLoader implements sc.obj.ISystemExitListener {
   AbstractInterpreter cmd;
   sc.layer.LayeredSystem sys; 
   List<URLPath> urlPaths; 

   public int waitForPageTime = 5000;
   public int waitForRuntimeTime = 5000;

   public boolean loadAllPages = true;

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
      for (URLPath urlPath:urlPaths) {
         if (urlPath.name.equals(name)) {
            res = loadURL(urlPath, scopeContextName);
            found = true;
            break;
         }
      }
      if (!found)
         throw new IllegalArgumentException("TestPageLoader.loadPage - " + name + " not found");
      return res;
   }

   public CurrentScopeContext loadPageAndWait(String pageName, String scopeContextName) {
       loadPage(pageName, scopeContextName);
       CurrentScopeContext ctx = cmd.waitForReady(scopeContextName, waitForPageTime);
       if (ctx == null)
          throw new AssertionError("TestPageLoader.loadPageAndWait(" + pageName + ", " + scopeContextName + ") - timed out waiting for connect");
       return ctx;
   }

   public void savePage(String name, int ix, String pageContents) {
      boolean found = false;
      for (URLPath urlPath:urlPaths) {
         if (urlPath.name.equals(name)) {
            saveURL(urlPath, getPageResultsFile(urlPath, ix), pageContents);
            found = true;
         }
      }
      if (!found)
         throw new IllegalArgumentException("TestPageLoader.savePage - " + name + " not found");
   }

   String getPageResultsFile(URLPath urlPath, int ix) {
      return FileUtil.concat(sys.options.testResultsDir, "pages", urlPath.name + (ix == -1 ? "" : "." + ix));
   }

   public AsyncProcessHandle loadURL(URLPath urlPath, String scopeContextName) {
      String pageResultsFile = getPageResultsFile(urlPath, -1);
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
            // for the inital page load, we just use the innerHTML which I think should be accurate? 
            saveURL(urlPath, pageResultsFile, getRemoteBodyHTML());
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
      if (processRes != null)
          processRes.endProcess();
   }

   // NOTE: using this for tests is not very robust because any changes made to the elements of the DOM are not reflected.  Instead, we'll
   // use the tag objects to generate the HTML output that reflects the page's current state. 
   public String getRemoteBodyHTML() {
      return (String) DynUtil.evalRemoteScript(AppGlobalScopeDefinition.getAppGlobalScope(), "document.body.innerHTML;");
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

         endSession(processRes);
         numLoaded++;
      }
      System.out.println("Done loading: " + numLoaded + " pages...");
   }

   public void systemExiting() {
      for (AsyncProcessHandle process:processes) {
         process.endProcess();
      }
      processes.clear();
   }
}

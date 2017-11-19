import sc.js.URLPath;
import java.util.List;
import sc.util.FileUtil;
import sc.obj.ScopeEnvironment;
import sc.obj.AppGlobalScopeDefinition;
import java.io.File;
import sc.dyn.DynUtil;
import sc.lang.AbstractInterpreter;

import sc.layer.AsyncResult;

public class TestPageLoader {
   AbstractInterpreter cmd;
   sc.layer.LayeredSystem sys; 
   List<URLPath> urlPaths; 
   boolean headless;

   boolean skipIndexPage = true;
   String chromeCmd = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

   public TestPageLoader(AbstractInterpreter cmd) {
      this.cmd = cmd;
      this.sys = cmd.system;
      this.urlPaths = sys.getURLPaths();
      this.headless = sys.options.headless;

      System.out.println("Waiting for server to start...");
      cmd.sleep(5000);
   }

   AsyncResult openBrowser(String url, String pageResultsFile) {
      AsyncResult processRes = null;
      if (headless) {
         if (sys.serverEnabled)
            processRes = cmd.execAsync('"' + chromeCmd + '"' + " --headless --auto-open-devtools-for-tabs --disable-gpu --repl --user-profile=/tmp/chrome-test-profile-dir " + url + " > /tmp/chromeHeadless.out");
         // client only hopefully we can just rely on chrome to save the dom with --dump-dom
         else {
            new File(pageResultsFile).getParentFile().mkdirs();
            processRes = cmd.execAsync('"' + chromeCmd + '"' + " --headless --disable-gpu --dump-dom --user-profile=/tmp/chrome-test-profile-dir " + url + " > " + pageResultsFile);
         }
      }
      else {
         cmd.exec("open " + url);
         cmd.sleep(2000); // give user time for opening devtools before starting any script
      }
      return processRes;
   }

   public AsyncResult loadPage(String name) {
      boolean found = false;
      AsyncResult res = null;
      for (URLPath urlPath:urlPaths) {
         if (urlPath.name.equals(name)) {
            res = loadURL(urlPath);
            found = true;
            break;
         }
      }
      if (!found)
         throw new IllegalArgumentException("TestPageLoader.loadPage - " + name + " not found");
      return res;
   }

   public void savePage(String name, int ix) {
      boolean found = false;
      for (URLPath urlPath:urlPaths) {
         if (urlPath.name.equals(name)) {
            saveURL(urlPath, getPageResultsFile(urlPath, ix));
            found = true;
         }
      }
      if (!found)
         throw new IllegalArgumentException("TestPageLoader.savePage - " + name + " not found");
   }

   String getPageResultsFile(URLPath urlPath, int ix) {
      return FileUtil.concat(sys.options.testResultsDir, "pages", urlPath.name + (ix == -1 ? "" : "." + ix));
   }

   public AsyncResult loadURL(URLPath urlPath) {
      String pageResultsFile = getPageResultsFile(urlPath, -1);
      System.out.println("Opening page: " + urlPath.name + " at: " + urlPath.url);

      // Returns file:// or http:// depending on whether the server is enabled.  Also finds the files in the first buildDir where it exists
      String loadUrl = sys.getURLForPath(urlPath.cleanURL(!sys.serverEnabled));

      System.out.println("Loading url: " + loadUrl);

      AsyncResult processRes = openBrowser(loadUrl, pageResultsFile);

      System.out.println("--- Waiting for client to connect...");
      cmd.sleep(1500);
      System.out.println("- Done waiting for client to connect");

      if (sys.serverEnabled) {
         saveURL(urlPath, pageResultsFile);
      }
      return processRes;
   }

   public void endSession(AsyncResult processRes) {
      if (processRes != null)
          processRes.endProcess();
   }

   void saveURL(URLPath urlPath, String pageResultsFile) {
      System.out.println("Getting DOM: " + urlPath.name);
      // Set the app-id so we restrict the contexts we search to just this application - theoretically, we could iterate over the sessions here too to target a specific browser instance to make it more robust
      ScopeEnvironment.setAppId(URLPath.getAppNameFromURL(urlPath.url));
      String res = (String) DynUtil.evalRemoteScript(AppGlobalScopeDefinition.getAppGlobalScope(), "document.body.innerHTML;");
      FileUtil.saveStringAsFile(pageResultsFile, res, true);
      System.out.println("- DOM results: " + urlPath.name + " length: " + res.length() + " path: " + pageResultsFile);
   }

   public void loadAllPages() {
      int numLoaded = 0;
      for (URLPath urlPath:urlPaths) {
         // Simple applications have only a single URL - the root.  Others have an index page and the application pages so we only skip when there's more than one
         if (skipIndexPage && urlPath.name.equals("index") && urlPaths.size() > 1)
            continue;
         AsyncResult processRes = loadURL(urlPath);
         endSession(processRes);
         numLoaded++;
      }
      System.out.println("Done loading: " + numLoaded + " pages...");
   }
}

package sc.html;

import sc.html.URL;
import sc.html.QueryParam;
import sc.html.URLParam;
import sc.html.MainInit;

import sc.lang.html.Element;
import sc.lang.html.MouseEvent;
import sc.lang.html.Window;
import sc.lang.html.Document;

/** 
 * This layer is the core of the HTML template package, shared by the servlet.schtml and js.schtml
 * layers.
 */
html.core extends sys.std {  // Extending sys.std because we override the standard file type handling.  Need to then have us after sys.std.
   compiledOnly = true;
   hidden = true;

   codeType = CodeType.Framework;

   public String defaultWebRoot = "web";

   object templateResourceLang extends TemplateLanguage {
      extensions = {"scxml"};
      // We will process or generate this file at build time - i.e. for static files or those used in the build.
      processTemplate = true;
      // For GWT, we used to do this in the 'prepare' phase because it's xml files need to be processed before you can 
      // compile.  But sys.std adds an scxml process in the process phase so the processors would both run so for now just
      // using process in the generic html.core layer.
      buildPhase = BuildPhase.Process;
      resultSuffix = "xml";
      useSrcDir = false;
      srcPathTypes = {null, "web"};

     // Share one buildDir for web root files since they do not support path searching
      //templateResourceLang.useCommonBuildDir = true;
      prependLayerPackage = false;
   }

   // Layers web files in the "doc" folder of any downstream layers
   object webFileProcessor extends LayerFileProcessor {
      // We do not use the layer's package in computing the web file names  An alternate design is to turn this off and pick
      // a fixed doc root.  This design ties paths in the doc root to types in Java.  I like this
      // design but where we can map the web root to some package in the hierarchy.
      prependLayerPackage = false;
      // Should we prepend the 'web' prefix onto .css and other files so they can live in the top-level layer folder?
      // Right now, this prefix gets added in the servlet.schtml, js.schtml and wicket.core libraries.
      // GWT requires a separate doc root which got messed up by this scheme so it is not set here.
      //webFileProcessor.templatePrefix = "web";
      useSrcDir = false;
      // Since jetty does not support path searching, all web files will be copied into the one common 'web' folder in the buildDir of the final layer
      // and that is where jetty starts.
      //webFileProcessor.useCommonBuildDir = true;
      processInAllLayers = true;
      srcPathTypes = {null, "web"};

      extensions = {"html", "jpg", "png", "gif", "pdf", "css", "js", "jsp", "xml", "properties", "tag", "svg"};
   }

   object cssLanguage extends sc.lang.CSSLanguage {
      extensions = {"sccss"};

      compiledTemplate = true;
      postBuildTemplate = true;

      // For a simpler implementation, when your css files are static for the site, just process them at compile time.  Won't allow access to runtime 
      // state but for this use case you don't need that.
      //processTemplate = true;

      // do generate the java class with the layer's package name
      prependLayerPackage = true;

      // But not the file generated in the process phase
      prependLayerPackageOnProcess = false;

      resultSuffix = "css";
      // Use processPrefix here so that the processedName is computed as a regular java type
      // name so we can replace sccss files with sc files.
      //processPrefix = "web";
      srcPathTypes = {null, "web"};

      // As a type we need the package but for saving the result file we do not (when compiledTemplate=true and processTemplate=true)
      prependLayerPackageOnProcess = false;
      useSrcDir = false;

      // Share one buildDir for web root files since they do not support path searching
      useCommonBuildDir = true;
      // To support more natural "code behind", sccss will do a modify by default of the previous file
      defaultModify = true;

      defaultExtendsType = "sc.lang.css.StyleSheet";
   }

   public void init() {
      LayeredSystem system = getLayeredSystem();
     
      // We use a singleton here for the IDE in particular so we can match parselets by identity
      sc.lang.HTMLLanguage htmlLang = sc.lang.HTMLLanguage.getHTMLLanguage();

      // Generating both an .html and a .java file from this type.  The html file is generated during the postBuild
      // phase - aftr the system has been built and those classes are loaded.
      htmlLang.compiledTemplate = true;
      htmlLang.postBuildTemplate = true;

      htmlLang.prependLayerPackage = true;
      // As a type we need the package but for saving the result file we do not (when compiledTemplate=true and processTemplate=true)
      htmlLang.prependLayerPackageOnProcess = false;

      //templateResourceLang.definedInLayer = this;
      htmlLang.buildPhase = sc.layer.BuildPhase.Process;
      htmlLang.resultSuffix = "html";
      htmlLang.useSrcDir = false;

      // Share one buildDir for web root files since they do not support path searching
      htmlLang.useCommonBuildDir = true;
      // To support more natural "code behind", schtml will do a modify by default of the previous file
      htmlLang.defaultModify = true;
      // for a file composed of for example just <html>...</html>, the type of the file is taken from the root element, not made a child of a "Page" class
      htmlLang.compressSingleElementTemplates = true;
      htmlLang.defaultExtendsType = "sc.lang.html.Page";
      // Do not try to generate .html files for types which do not have <html> or otherwise indicate they are addressed by a URL or have MainInit set on them.
      htmlLang.processOnlyURLs = true;
      htmlLang.srcPathTypes = new String[] {null, "web"};
      // The .sc and .schtml files replace each other in the type system - i.e. not part of the "processed id" which lets one file in a subsequent layer from processing that file in the next layer
      // the suffix will be .java when compiledTemplate is true and one of process or postBuild template
      //htmlLang.processByUniqueSuffix = true;
      registerLanguage(htmlLang, sc.lang.HTMLLanguage.SC_HTML_SUFFIX);
   }

   public void start() {
      sc.layer.LayeredSystem system = getLayeredSystem();

      // Add this here in core even though it's used in schtml so that it's always in front of js.core where the sc.tag package is added.
      system.addTagPackageDirectory("sc.html.tag", this, 1);

      String openSuffix = system.options.openPattern;
      if (openSuffix == null)
         openSuffix = system.serverEnabled ? "" : "index.html";

      String webURL = system.getServerURL() + openSuffix;

      if (activated) {
         // Open up to the index page for this layer or use the -o URL option to open a specific page.
         if (system.options.openPageAtStartup)
            system.addRunCommand(sc.type.PTypeUtil.getPlatformOpenCommand(),  webURL);

         // Use the -ts <script-name> option to specify a test script - if not specified, we default to testScript.scr
         if (system.options.testScriptName == null)
            system.options.testScriptName = "testScript.scr";

         // Add a shell script to use with the -t option and another to
         // use with -vt (test verify mode)
         //system.addTestCommand(system.options.testVerifyMode ? "./autoTest.sh" : "./runTest.sh");

         // Warn the user if there's no upstream layer for either javascript or a server implementation
         if (system.getLayerByDirName("js.allInOne") == null && system.getLayerByDirName("servlet.schtml") == null && system.getLayerByDirName("wicket.core") == null && system.getLayerByDirName("html.schtml") == null && system.getLayerByDirName("js.schtml") == null) {
            System.err.println("*** Warning: html.core layer included without html.schtml, js.schtml, servlet.schtml, and js.allInOne.  Include js.schtml for a client, servlet.schtml for a server or both for a synchronized implementation. Use js.allInOne for building a single JS app - all in one file. By itself, html.core provides an API for HTML generation but does not even generate a default HTML file.");
         }
      }

      sc.lang.DefaultAnnotationProcessor urlProc = new sc.lang.DefaultAnnotationProcessor();
      // Need to add a static code snippet to register the page.  If we happen to register an inner class the addPage still goes on the parent type
      urlProc.validOnField = false;
      urlProc.validOnClass = true;
      urlProc.validOnObject = true;
      urlProc.initOnStartup = true;
      urlProc.typeGroupName = "URLTypes";
      urlProc.inherited = true; // Include any sub-type which has URL in the type group
      urlProc.skipAbstract = true; // Don't include any abstract macro templates
      registerAnnotationProcessor("sc.html.URL", urlProc);

      sc.lang.DefaultAnnotationProcessor mainInitProc = new sc.lang.DefaultAnnotationProcessor();
      mainInitProc.typeGroupName = "mainInit";
      mainInitProc.validOnField = false;
      mainInitProc.validOnObject = true;
      mainInitProc.inherited = true; // Include any sub-type which has MainInit
      mainInitProc.skipAbstract = true; // Don't include any abstract macro templates
      mainInitProc.subTypesOnly = true; // Don't include the Html and Page types which set the annotation
      registerAnnotationProcessor("sc.html.MainInit", mainInitProc);

      if (activated) {
         // When either of these type groups change, we need to regenerate the runTest script
         addTypeGroupDependency("runTest.scsh", "runTest", "mainInit");
         addTypeGroupDependency("runTest.scsh", "runTest", "urlTypes");
      }
   }
}

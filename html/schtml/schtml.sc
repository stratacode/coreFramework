import sc.html.EditablePage;

/** 
   html.schtml defines default tag objects for the sc.html.tag package that provide nice defaults for
   schtml applications - e.g. session scope pages, load the JS after the body etc.  Most apps will extend this
   layer unless they require special JS semantics like the js.allInOne mode - which runs the entire app like a java 'main"
   loaded into the index page.  
   */
html.schtml extends html.core {
   exportPackage = false;
   compiledOnly = true;
   hidden = true;

   codeType = sc.layer.CodeType.Framework;

   {
      // We replace the html.core feature - so that any layers which extend html.core are placed after us in the stack.
      // This must be done in class initialize since it affects the stacking order.
      replacesLayer("html.core");
   }

   webFileProcessor {
      templatePrefix = defaultWebRoot;
   }

   void start() { 
      sc.layer.LayeredSystem system = getLayeredSystem();
      sc.lang.TemplateLanguage tempLang = (sc.lang.TemplateLanguage) system.getFileProcessorForExtension("schtml");
      //tempLang.processPrefix = "web";

      // Note: this sets the templatePrefix for all web files since there's one processor used for all of them.
      sc.layer.LayerFileProcessor webProc = (sc.layer.LayerFileProcessor) system.getFileProcessorForExtension("css");
      //webProc.templatePrefix = "web";

      // Any files under the 'web' directory are of srcPathType = 'web'. 
      addSrcPath("web", "web", defaultWebRoot, null);
   }
}

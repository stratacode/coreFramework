public servlet.schtml extends core, html.schtml, webApp {
   compiledOnly = true;

   codeType = sc.layer.CodeType.Framework;
   hidden = true;

   public void start() {
      sc.layer.LayeredSystem system = getLayeredSystem();
      sc.lang.TemplateLanguage tempLang = (sc.lang.TemplateLanguage) system.getFileProcessorForExtension("schtml");
      //tempLang.processPrefix = "web";

      // Note: this sets the templatePrefix for all web files since there's one processor used for all of them.
      sc.layer.LayerFileProcessor webProc = (sc.layer.LayerFileProcessor) system.getFileProcessorForExtension("css");
      //webProc.templatePrefix = "web";

      addSrcPath("web", "web");

      // Causes PageInit.sc to be regenerated whenever the members of either type group are modified
      addTypeGroupDependency("PageInit.sc", "sc.servlet.PageInit", "_init");
      addTypeGroupDependency("PageInit.sc", "sc.servlet.PageInit", "_startup");
   }
}

public gui.util.html extends gui.util.core, html.core {
   codeType = sc.layer.CodeType.UI;

   void init() {
      // this conflicts with swing's version so need to add this dependency
      excludeProcess("Desktop");
   }
}

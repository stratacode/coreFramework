public gui.util.html extends gui.util.core, html.core {
   codeType = sc.layer.CodeType.Framework;

   void init() {
      // this conflicts with swing's version so need to add this dependency
      excludeProcess("Desktop");
   }
}

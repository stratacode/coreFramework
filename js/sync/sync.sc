package sc.js;

public js.sync extends js.core, js.math.core {
   compiledOnly = true;
   hidden = true;
   codeType = sc.layer.CodeType.Framework;

   public void init() {
      excludeRuntimes("java", "gwt", "android");

      if (activated)
         layeredSystem.syncEnabled = true;
   }

   public void start() {
      sc.layer.LayeredSystem system = getLayeredSystem();

      if (system.runtimeProcessor instanceof sc.lang.js.JSRuntimeProcessor)
          ((sc.lang.js.JSRuntimeProcessor) system.runtimeProcessor).destinationName = "jsHttp";
   }
}

package java.awt;

import sc.obj.Constant;

awt.meta {
   codeType = sc.layer.CodeType.Framework;
   codeFunction = sc.layer.CodeFunction.UI;

   compiledOnly = true;
   annotationLayer = true;

   public void init() {
      // Exclude the javascript runtime.  All layers which extend this layer explicitly will also be excluded, unless they explicitly include a layer which uses JS
      excludeRuntimes("js", "android", "gwt");

      // Awt requires the default runtime, Desktop process
      //addRuntime(null);
      addProcess(sc.layer.ProcessDefinition.create("Desktop"));
   }
}

servlet.lib {
   compiledOnly = true;
   hidden = true;

   codeType = sc.layer.CodeType.Framework;

   object servletPkg extends MvnRepositoryPackage {
      url = "mvn://javax.servlet/javax.servlet-api/3.1.0";
   }

   // Runs when the layer is initialized
   public void init() {
      // Exclude this layer from these runtimes. 
      excludeRuntimes("js", "gwt", "android");

      addProcess(sc.layer.ProcessDefinition.create("Server", "java", true));
   }

   // override start() to run code once all layers have been initialized
   // override validate() to run code once all layers have been started
}

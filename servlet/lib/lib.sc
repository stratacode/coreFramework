servlet.lib {

   compiledOnly = true;
   hidden = true;

   codeType = sc.layer.CodeType.Framework;

   object servletPkg extends MvnRepositoryPackage {
      url = "mvn://javax.servlet/javax.servlet-api/3.1.0";
   }

   public void init() {
      // Exclude the javascript runtime.  All layers which extend this layer explicitly will also be excluded, unless they explicitly include a layer which uses JS
      excludeRuntimes("js", "gwt", "android");

      addProcess(sc.layer.ProcessDefinition.create("Server", "java", true));
   }
}

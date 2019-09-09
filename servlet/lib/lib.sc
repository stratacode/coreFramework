servlet.lib {

   compiledOnly = true;
   hidden = true;

   codeType = sc.layer.CodeType.Framework;

   object servletPkg extends MvnRepositoryPackage {
      url = "mvn://javax.servlet/javax.servlet-api/3.1.0";
   }

}

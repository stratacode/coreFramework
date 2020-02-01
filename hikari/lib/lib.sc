hikari.lib {
   compiledOnly = true;
   hidden = true;

   codeType = sc.layer.CodeType.Framework;

   object hikariCPPkg extends MvnRepositoryPackage {
      url = "mvn://com.zaxxer/HikariCP/3.4.1";
   }
}

jetty.webApp extends jetty.servlet {
   compiledOnly = true;
   hidden = true;

   codeType = CodeType.Framework;

   // TODO: if necessary break this out into a separate layer. Or merge this entire layer into jetty.servlet
   object hikariCPPkg extends MvnRepositoryPackage {
      url = "mvn://com.zaxxer/HikariCP/3.4.1";
   }
}

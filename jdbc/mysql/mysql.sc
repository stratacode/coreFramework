jdbc.mysql {
   public void start() {
      String componentName = "mysql-connector-java-";
      String version = "5.1.35";
      sc.layer.LayeredSystem system = getLayeredSystem();

      //sc.repos.RepositoryPackage pkg = addRepositoryPackage("jettyLibs", "scp", "vsgit@stratacode.com:/home/vsgit/jettyLibs", false);
      //sc.repos.RepositoryPackage pkg = addRepositoryPackage("mysqlJDBC", "url", "http://stratacode.com/packages/" + componentName + version + ".zip", true);
      sc.repos.RepositoryPackage pkg = addRepositoryPackage("mvn://mysql/mysql-connector-java/5.1.6");

      if (pkg.installedRoot != null && !disabled) {
         classPath = pkg.classPath;
      }
   }
}

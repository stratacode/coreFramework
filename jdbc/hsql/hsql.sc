jdbc.hsql {
   public void start() {
      LayeredSystem system = getLayeredSystem();

      RepositoryPackage pkg = addRepositoryPackage("mvn://org.hsqldb/hsqldb/2.3.2");
      if (pkg.installedRoot != null && !disabled) {
         classPath = pkg.classPath;
      }
   }
}

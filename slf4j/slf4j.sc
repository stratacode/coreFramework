slf4j {
   public void start() {
      LayeredSystem system = getLayeredSystem();

      RepositoryPackage pkg = addRepositoryPackage("mvn://org.slf4j/slf4j-api/1.7.12");
      if (pkg.installedRoot != null && !disabled) {
         classPath = pkg.classPath;
      }
      pkg = addRepositoryPackage("mvn://org.slf4j/slf4j-simple/1.7.12");
      if (pkg.installedRoot != null && !disabled) {
         classPath += FileUtil.PATH_SEPARATOR + pkg.classPath;
      }
   }
}

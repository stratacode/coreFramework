
jdbc.pgsql extends db.model {
   compiledOnly = true;
   hidden = true;

   codeType = CodeType.Framework;

   String pgsqlJDBCVersion = "42.2.9";

   void init() {
      excludeRuntimes("js", "gwt", "android");
   }

   void start() {
      String jarName = "postgresql-" + pgsqlJDBCVersion + ".jar";
      RepositoryPackage pkg = addRepositoryPackage("pgsqlJDBC", jarName, "url", "https://jdbc.postgresql.org/download/" + jarName, false);
      if (pkg.installedRoot != null && !disabled) {
         classPath = pkg.classPath;
      }

      DBProvider pgProvider = new DBProvider("postgresql");
      layeredSystem.addDBProvider(pgProvider, this);
      layeredSystem.defaultDBProvider = pgProvider;
   }
}

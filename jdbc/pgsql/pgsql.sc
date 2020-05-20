
jdbc.pgsql extends db.model {
   compiledOnly = true;
   hidden = true;

   codeType = CodeType.Framework;

   String pgsqlJDBCVersion = "42.2.9";

   public void start() {
      String jarName = "postgresql-" + pgsqlJDBCVersion + ".jar";
      RepositoryPackage pkg = addRepositoryPackage("pgsqlJDBC", jarName, "url", "https://jdbc.postgresql.org/download/" + jarName, false);
      if (pkg.installedRoot != null && !disabled) {
         classPath = pkg.classPath;
      }

      layeredSystem.addDBProvider(new DBProvider("postgresql"), this);
   }
}

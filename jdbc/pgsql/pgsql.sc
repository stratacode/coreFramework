import sc.db.DBTypeDescriptor;
import sc.db.DBPropertyDescriptor;
import sc.db.FindBy;
import sc.db.DBTypeSettings;
import sc.db.DBPropertySettings;
import sc.db.DBUtil;

jdbc.pgsql {
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

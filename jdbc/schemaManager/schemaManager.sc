package sc.jdbc;

public jdbc.schemaManager extends jdbc.pgsql, sys.std {
   hidden = true;
   compiledOnly = true;
   codeType = sc.layer.CodeType.Framework;

   void init() {
      // TODO: maybe have this run in a separate process, so we can give it separate permissions and 
      // have it attach and update the schema after we build and before run the application.
      //addProcess(sc.layer.ProcessDefinition.create("Server", "java", true));

      // Treat files in the sql directory as resources
      addSrcPath("sql", "resource");
   }

   void process() {
      if (activated) {
         checkAndCreateDB();
      }
   }

   void checkAndCreateDB() {
      if (layeredSystem.activeDataSources == null)
        return;
      for (DBDataSource ds:layeredSystem.activeDataSources) {
         String checkFileName = FileUtil.concat(LayerUtil.getDeployedDBSchemasDir(layeredSystem), ds.dbName + ".check");
         if (!new File(checkFileName).canRead()) {
            do {
               System.out.println("Performing check for database: " + ds.dbName + " for build layer: " + layeredSystem.buildLayer);
               java.util.Map<String,String> env = new java.util.TreeMap<String,String>();
               env.put("PGPASSWORD", ds.password);
               String res = FileUtil.exec(null, env, true, "psql", "-h", ds.serverName, "-p", String.valueOf(ds.port), "-U", ds.userName, ds.dbName, "-c", "select 1;"); // Can we connect to the db server
               if (res != null) {
                  System.out.println("Database found: " + ds.dbName);
                  FileUtil.saveStringAsFile(checkFileName, new java.util.Date().toString(), true);
                  break;
               }
               else {
                  System.err.println("Connection to database: " + ds.dbName + " failed for user: " + ds.userName);
                  System.out.println("To create the database user run: ");
                  System.out.println(" createuser -U postgres -P " + ds.userName);
                  System.out.println("    (First enter sudo password, then password for " + ds.userName + ", then for postgres admin account) ");
                  System.out.println("Then create the database, owned by scserver that will create and update the schema: ");
                  System.out.println(" createdb -U postgres -O " + ds.userName + " " + ds.dbName);
                  if (layeredSystem.cmd != null) {
                     String input = layeredSystem.cmd.readLine("Ready to test again? (y/n): ");
                     if (!input.equalsIgnoreCase("y") && !input.equalsIgnoreCase("yes")) {
                        break;
                     }
                  }
               }
            } while (true);
         }
      }
   }
}

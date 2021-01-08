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
            System.out.println("First time accessing database: " + ds.dbName + " for build layer: " + layeredSystem.buildLayer + " - try to connect to DB");
            String res = FileUtil.exec(null, true, "psql", "-h", ds.serverName, "-p", String.valueOf(ds.port), "-U", ds.userName, ds.dbName, "-c", "select 1;"); // Can we connect to the db server
            if (res != null) {
               FileUtil.saveStringAsFile(checkFileName, new java.util.Date().toString(), true);
            }
            else {
               System.err.println("*** Not able to connect to database: " + ds.dbName + " with user: " + ds.userName + " found - will try to create it:");
               res = FileUtil.exec(null, true, "psql","-U", ds.userName, "-c", "create database " + ds.dbName + ";");
               if (res == null) {
                  System.err.println("*** Failed to create database: " + ds.dbName);
               }
               else {
                  System.out.println("Created database: " + ds.dbName + " successfully");
                  FileUtil.saveStringAsFile(checkFileName, new java.util.Date().toString(), true);
               }
            }
         }
      }
   }
}

package sc.jdbc;

public jdbc.schemaManager extends jdbc.pgsql {
   void init() {
      // TODO: maybe have this run in a separate process, so we can give it separate permissions and 
      // have it attach and update the schema after we build and before run the application.
      //addProcess(sc.layer.ProcessDefinition.create("Server", "java", true));
   }
}
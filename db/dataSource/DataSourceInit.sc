import java.util.HashMap;

import sc.db.DataSourceManager;
import sc.db.DBDataSource;

@sc.obj.CompilerSettings(mixinTemplate="sc.sql.DataSourceInitTemplate",initOnStartup=true)
class DataSourceInit {

   static DBDataSource addDBDataSource(String jndiName, String dbName, String userName, String password, String serverName, int port, boolean readOnly, boolean dbDisabled) {
      DBDataSource res = new DBDataSource();
      res.jndiName = jndiName;
      res.dbName = dbName;
      res.userName = userName; 
      res.password = password;
      res.serverName = serverName;
      res.port = port;
      res.readOnly = readOnly;
      res.dbDisabled = dbDisabled;
      DataSourceManager.addDBDataSource(jndiName, res);
      return res;
   }
}

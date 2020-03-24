import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import javax.naming.InitialContext;

import java.util.HashMap;

import sc.db.DataSourceManager;

DataSourceInit {
   static DBDataSource addDBDataSource(String jndiName, String dbName, String userName, String password, String serverName, int port, boolean readOnly, boolean dbDisabled) {
      DBDataSource dbDS = super.addDBDataSource(jndiName, dbName, userName, password, serverName, port, readOnly, dbDisabled);

      HikariConfig conf = new HikariConfig();
      conf.setJdbcUrl("jdbc:postgresql://" + serverName + ":" + port + "/" + dbName);
      conf.setUsername(userName);
      conf.setPassword(password);
      //conf.addDataSourceProperty("cachePrepStmts", "true");
      //conf.addDataSourceProperty("prepStmtCacheSize", "250");
      //conf.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
      DataSource ds = new HikariDataSource(conf);
      dbDS.dataSource = ds;

      return dbDS;
   }
}

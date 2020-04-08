import sc.db.ISchemaUpdater;
import sc.db.DataSourceManager;
import sc.db.DBSchemaType;
import sc.db.DBSchemaVersion;
import sc.db.DBUtil;
import sc.db.DBDataSource;
import sc.db.DBMetadata;
import sc.db.ColumnInfo;
import sc.db.TableInfo;
import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.InputStream;

@sc.obj.CompilerSettings(initOnStartup=true)
object SchemaUpdater implements ISchemaUpdater {
   static {
      DataSourceManager.addSchemaUpdater("postgresql", SchemaUpdater);
   }

   private static String createTablesSchemaStr = null;

   public boolean enableMetadataValidation = true;

   void setSchemaReady(String dataSourceName, boolean val) {
      DBDataSource dbDS = DataSourceManager.getDBDataSource(dataSourceName);
      if (dbDS != null)
         dbDS.setSchemaReady(val);
      else
         DBUtil.error("setSchemaReady - no dataSource: " + dataSourceName);
   }

   List<DBSchemaType> getDBSchemas(String dataSourceName) {
      Connection conn = null;
      PreparedStatement st = null;
      ResultSet rs = null;
      try {
         conn = DBUtil.createConnection(dataSourceName);
         // Get the current version of all of the schema types
         st = conn.prepareStatement("SELECT tst.id, tst.type_name, tsv.id, tsv.schema_sql, tsv.alter_sql, tsv.date_applied, tsv.version_info " +
                                    "FROM db_schema_type AS tst, db_schema_current_version AS tscv, db_schema_version AS tsv " +
                                    "WHERE tst.id = tscv.schema_type AND " +
                                    "      tscv.schema_version = tsv.id");

         rs = st.executeQuery();

         ArrayList<DBSchemaType> res = new ArrayList<DBSchemaType>();

         while (rs.next()) {
            DBSchemaType newType = new DBSchemaType();
            DBSchemaVersion newVers = new DBSchemaVersion();
            newType.setCurrentVersion(newVers);

            int col = 1;
            newType.setId(rs.getLong(col++));
            newType.setTypeName(rs.getString(col++));
            newVers.setId(rs.getLong(col++));
            newVers.setSchemaSQL(rs.getString(col++));
            newVers.setAlterSQL(rs.getString(col++));
            newVers.setDateApplied(rs.getTimestamp(col++));
            newVers.setVersionInfo(rs.getString(col++));

            res.add(newType);
         }

         return res;
      }
      catch (SQLException exc) {
         return null;
      }
      finally {
         DBUtil.close(conn, st, rs);
      }
   }
   void applyAlterCommands(String dataSourceName, List<String> alterCommands) {
      Connection conn = null;
      PreparedStatement st = null;
      ResultSet rs = null;
      String alterCmd = null;
      try {
         conn = DBUtil.createConnection(dataSourceName);

         for (int i = 0; i < alterCommands.size(); i++) {
            alterCmd = alterCommands.get(i);
            st = conn.prepareStatement(alterCmd);

            if (DBUtil.verbose)
               DBUtil.verbose("-- Exec sql update:\n" + alterCmd);
            int rowCt = st.executeUpdate();
            /*
            if (rowCt == 0)
               System.err.println("*** Invalid returned row count");
            else
            */
         }
         DBUtil.verbose("Completed: " + alterCommands.size() + " sql statements");
      }
      catch (SQLException exc) {
         throw new IllegalArgumentException("DB error applying schema update cmd:\n" + alterCmd + "\n", exc);
      }
      finally {
         DBUtil.close(conn, st);
      }
   }

   void updateDBSchemaForType(String dataSourceName, DBSchemaType info) {
      Connection conn = null;
      PreparedStatement st = null;
      ResultSet rs = null;
      try {
         conn = DBUtil.createConnection(dataSourceName);

         boolean newSchemaType = false;

         // Both find the id of the existing type and initialize the db_schema_type etc. tables if they don't exist
         DBSchemaType curType = findSchemaType(conn, info.getTypeName(), true);
         long typeId;
         if (curType == null) {
            st = conn.prepareStatement("INSERT INTO db_schema_type(type_name) VALUES(?) RETURNING id");
            st.setString(1, info.typeName);
            rs = st.executeQuery();
            if (!rs.next())
               throw new UnsupportedOperationException("Invalid return from insert");
            typeId = rs.getLong(1);

            rs.close();
            st.close();
            st = null;
            rs = null;
            newSchemaType = true;
         }
         else
            typeId = curType.id;

         st = conn.prepareStatement("INSERT INTO db_schema_version(schema_type, schema_sql, alter_sql, version_info, build_layer_name) " +
                                    "VALUES(?, ?, ?, ?, ?) RETURNING id");
         int col = 1;
         st.setLong(col++, typeId);
         DBSchemaVersion vers = info.getCurrentVersion();
         st.setString(col++, vers.schemaSQL);
         st.setString(col++, vers.alterSQL);
         st.setString(col++, vers.versionInfo);
         st.setString(col++, vers.buildLayerName);

         rs = st.executeQuery();
         if (!rs.next())
            throw new UnsupportedOperationException("Invalid return from insert version");
         long versionId = rs.getLong(1);

         rs.close();
         st.close();
         st = null;
         rs = null;

         if (newSchemaType) {
            st = conn.prepareStatement("INSERT INTO db_schema_current_version(schema_type, schema_version) VALUES(?, ?)");
            st.setLong(1, typeId);
            st.setLong(2, versionId);
         }
         else {
            st = conn.prepareStatement("UPDATE db_schema_current_version SET schema_version = ? WHERE schema_type = ?");
            st.setLong(1, versionId);
            st.setLong(2, typeId);
         }
         int resCt = st.executeUpdate();
         if (resCt != 1) {
            if (newSchemaType)
               System.err.println("*** Invalid update row count from INSERT new schema version");
            else {
               st.close();
               st = conn.prepareStatement("INSERT INTO db_schema_current_version(schema_type, schema_version) VALUES(?, ?)");
               st.setLong(1, typeId);
               st.setLong(2, versionId);
               resCt = st.executeUpdate();
               if (resCt != 1) {
                  System.err.println("*** Invalid updated row count after retry of INSERT for new schema version");
               }
            }
         }
         // TODO: currently using autoCommit but this would be a good transaction boundary
         //conn.commit();
      }
      catch (SQLException exc) {
         throw new IllegalArgumentException("Failed to updateDB schema - SQLException: " + exc + " for statement: " + st);
      }
      finally {
         DBUtil.close(conn, st);
      }
   }

   void removeDBSchemaForType(String dataSourceName, String typeName) {
      Connection conn = null;
      PreparedStatement st = null;
      try {
         conn = DBUtil.createConnection(dataSourceName);

         DBSchemaType curType = findSchemaType(conn, typeName, false);
         if (curType != null) {
            st = conn.prepareStatement("UPDATE db_schema_current_version SET schema_version = NULL WHERE schema_type = ?");
            st.setLong(1, curType.getId());
         }
         DBUtil.info("Removed db_schema_current_version for: " + typeName);
      }
      catch (SQLException exc) {
         DBUtil.error("DB error: " + exc + " for removeDBSchemaForType(" + dataSourceName + ", " + typeName + ")");
      }
      finally {
         DBUtil.close(conn, st);
      }
   }

   private DBSchemaType findSchemaType(Connection conn, String typeName, boolean initSchemaOnError) {
      PreparedStatement st = null;
      ResultSet rs = null;
      try {
         st = conn.prepareStatement("SELECT id FROM db_schema_type WHERE type_name = ?");
         st.setString(1, typeName);
         rs = st.executeQuery();
         if (rs.next()) {
            DBSchemaType res = new DBSchemaType();
            res.setId(rs.getLong(1));
            res.setTypeName(typeName);
            return res;
         }
      }
      catch (SQLException exc) {
         if (initSchemaOnError)
            initSchema(conn);
      }
      finally {
         DBUtil.close(null, st, rs);
      }
      return null;

   }

   private void initSchema(Connection conn) {
      PreparedStatement st = null;
      try {
         st = conn.prepareStatement(getCreateTablesSchema());
         int ct = st.executeUpdate();
         System.out.println("*** Result count is: " + ct);
      }
      catch (SQLException exc) {
      }
      finally {
         DBUtil.close(null, st, null);
      }
   }

   private static String getCreateTablesSchema() {
      if (createTablesSchemaStr != null)
         return createTablesSchemaStr;
      InputStream is = SchemaUpdater.class.getClassLoader().getResourceAsStream("typeInfo.sql");
      if (is == null)
         throw new IllegalArgumentException("Missing typeInfo.sql in classpath for SchemaUpdater");
      StringBuilder sb = DBUtil.readInputStream(is);
      if (sb == null)
         return null;
      createTablesSchemaStr = sb.toString();
      return createTablesSchema;
   }


   static ColumnInfo createFromResultSet(ResultSet rs) throws SQLException {
      ColumnInfo info = new ColumnInfo();
      info.colName = rs.getString("COLUMN_NAME");
      info.colType = rs.getInt("DATA_TYPE");
      info.size = rs.getString("COLUMN_SIZE");
      info.numDigits = rs.getInt("DECIMAL_DIGITS");
      String isNullStr = rs.getString("IS_NULLABLE");
      info.isNullable = isNullStr != null && isNullStr.equalsIgnoreCase("yes");
      return info;
   }

   public DBMetadata getDBMetadata(String dataSourceName) {
      DBMetadata dbmd = new DBMetadata();
      List<TableInfo> tableInfos = new ArrayList<TableInfo>();
      dbmd.tableInfos = tableInfos;

      Connection conn = null;
      ResultSet rs = null;

      try {
         conn = DBUtil.createConnection(dataSourceName);

         DatabaseMetaData jmd = conn.getMetaData();
         rs = jmd.getTables(null, null, null, new String[] {"TABLE"});
         while (rs.next()) {
            String tableName = rs.getString("table_name");

            TableInfo tableInfo = new TableInfo(tableName);
            tableInfos.add(tableInfo);
         }
         rs.close();
         rs = null;

         for (TableInfo tableInfo:tableInfos) {
            rs = jmd.getColumns(null, null, tableInfo.tableName, null);
            List<ColumnInfo> colInfos = tableInfo.colInfos;
            while (rs.next()) {
               ColumnInfo info = createFromResultSet(rs);
               colInfos.add(info);
            }
            rs.close();
            rs = null;
         }
         return dbmd;
      }
      catch (SQLException exc) {
         DBUtil.error("Error extracting metadata from existing dataSource: " + dataSourceName + ": " + exc);
         return null;
      }
      finally {
         DBUtil.close(conn, null, rs);
      }
   }
}

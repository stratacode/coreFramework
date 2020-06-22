package sc.sql;

import sc.db.DBDataSource;

public db.dataSource {
   compiledOnly = true;
   hidden = true;

   codeType = sc.layer.CodeType.Framework;

   void init() {
      excludeRuntimes("js", "gwt", "android");
   }
}

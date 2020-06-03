function sc_IDBObject() {}
sc_IDBObject_c = sc_newClass("sc.obj.IDBObject", sc_IDBObject, null, null);

function sc_DBObject(wrap) {
   this.wrapper = wrap;
}
sc_DBObject_c = sc_newClass("sc.obj.DBObject", sc_DBObject, sc_IDBObject, null);

sc_DBObject_c.init = function() {}

sc_DBObject_c.dbInsert = function(q) {
   console.log("dbInsert not yet implemented");
}

sc_DBObject_c.dbDelete = function(q) {
   console.log("dbDelete not yet implemented");
}

sc_DBObject_c.dbValidate = function(q) {
   console.log("dbValidate not yet implemented");
}

sc_DBObject_c.dbUpdate = function() {
   console.log("dbUpdate not yet implemented");
}

sc_DBObject_c.dbRefresh = function() {
   console.log("dbRefresh not yet implemented");
}

sc_DBObject_c.getDBObject = function() {
   return this;
}

sc_DBObject_c.getDBId = function() {
   return wrap.id; // TODO: this should use the id property name for the object
}



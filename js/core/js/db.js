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
sc_DBObject_c.getObjectId = function() {
    if (this.wrapper.id == 0 || !this.wrapper.id) {
       console.error("transient instance in getObjectId on client")
    }
   return sc_CTypeUtil_c.getClassName(sc_DynUtil_c.getTypeName(sc_DynUtil_c.getType(this.wrapper))) + "__" + this.wrapper.id;
}

sc_DBObject_c.getDBId = function() {
   return this.wrapper.id; // TODO: this should use the id property name for the object
}

// TODO: insert basic serialization for any DB metadata we need in the client
function sc_DBTypeDescriptor() {}

sc_DBTypeDescriptor_c = sc_newClass("sc.db.DBTypeDescriptor", sc_DBTypeDescriptor, null, null);

sc_DBTypeDescriptor_c.getByType = function(type) {
   return null;
}

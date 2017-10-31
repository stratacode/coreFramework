var sc$instancesByType = new Object();
var sc$objectIds = new Object();
var sc$typeIdCounts = new Object();
var sc$dynListeners = null;
var sc$propNameTable = new Object();

if (!Array.isArray) {
   Array.isArray = function (arg) {
      return Object.prototype.toString.call(arg) === "[object Array]";
   }
}

function sc_DynUtil() {}

sc_DynUtil_c = sc_newClass("sc.dyn.DynUtil", sc_DynUtil, null, null);
sc_DynUtil_c.getName = jv_Class_c.getName;

sc_DynUtil_c.addDynObject = function(typeName, inst) {
   inst.$objectName = typeName;
   inst.$objectType = true;
   sc_DynUtil_c.addDynInstance(typeName, inst);
}

sc_DynUtil_c.addDynInstance = function(typeName, inst) {
   if (!sc$liveDynamicTypes) return;
   var insts = sc$instancesByType[typeName];
   if (insts === undefined) {
      sc$instancesByType[typeName] = insts = new Object();
   }
   insts[sc_id(inst)] = inst;

   if (sc$dynListeners != null) {
      for (var i = 0; i < sc$dynListeners.length; i++)
         sc$dynListeners[i].instanceAdded(inst);
   }
}

sc_DynUtil_c.addDynInnerObject = function(typeName, inst, outer) {
   if (!sc$liveDynamicTypes) return;
   inst.$objectName = sc_CTypeUtil_c.getClassName(typeName);
   inst.$objectType = true;
   sc_DynUtil_c.addDynInnerInstance(typeName, inst, outer);
}

sc_DynUtil_c.addDynInnerInstance = function(typeName, inst, outer) {
   if (inst.outer == null) // For objects simple enough where there's no class and the type of the instance itself is not an inner type, we set the outer type of the instance here
      inst.outer = outer;
   sc_DynUtil_c.addDynInstance(typeName, inst);
}

sc_DynUtil_c.addDynListener = function(listener) {
   if (sc$dynListeners == null)
      sc$dynListeners = [];
   sc$dynListeners.push(listener);
}

sc_DynUtil_c.removeDynListener = function(listener) {
   var ix;
   if (sc$dynListeners == null || (ix = sc$dynListeners.indexOf(listener)) == -1)
      console.error("No dynListener to remove");
   else
      sc$dynListeners.splice(ix, 1);
}

sc_DynUtil_c.initComponent = function(c) {
  c.init();
}

sc_DynUtil_c.startComponent = function(c) {
  c.start();
}

sc_DynUtil_c.intValue = function(v) {
   if (v == null)
      return 0;
   return v;
}

sc_DynUtil_c.booleanValue = function(v) {
   if (v == null)
      return false;
   return v;
}

sc_DynUtil_c.floatValue = sc_DynUtil_c.doubleValue = function(v) {
   if (v == null)
      return 0.0;
   return v;
}

sc_DynUtil_c.getType = sc_DynUtil_c.getSType = function(o) {
   if (o == null)
      console.error("*** Error - null type");
   return o.constructor.prototype;
}

sc_DynUtil_c.getTypeOfObj = function(o) {
    if (sc_DynUtil_c.isType(o)) // In JS the getClass() method does not work on the _c class object but we need this for getting the right sync behavior of the class as an instance
       return jv_Class_c;
   else
       return sc_DynUtil_c.getType(o);
}

sc_DynUtil_c.resolveMethod = function(vartype, methName, sig) {
   return {type:sc_clInit(vartype), name:methName, paramSig:sig};
}

sc_DynUtil_c.resolveRemoteMethod = function(vartype, methName, sig) {
   var res = sc_DynUtil_c.resolveMethod(vartype, methName, sig);
   res.remote = true;
   return res;
}

sc_DynUtil_c.isRemoteMethod = function(meth) {
   return meth.remote == true;
}

sc_DynUtil_c.resolveStaticMethod = function(vartype, methName) {
   return {type:sc_clInit(vartype), name:methName, methStatic:true};
}

sc_DynUtil_c.resolveStaticRemoteMethod = function(vartype, methName) {
   var res = sc_DynUtil_c.resolveStaticMethod(vartype, methName);
   res.remote = true;
   return res;
}

sc_DynUtil_c.getMethodName = function(method) {
   return method.name;
}

sc_DynUtil_c.getParameterTypes = function() { return null; }

sc_DynUtil_c.invokeMethod = function(obj, method, paramValues) {
   if (method.methStatic)
      return method.type[method.name].apply(null, paramValues);
   else {
      if (obj[method.name] == null) {
         console.log("Error no method: " + method.name + " defined for obj: " + sc_DynUtil_c.getInstanceName(obj));
         throw new jv_IllegalArgumentException("No method named: " + method.name);
      }
      else {
         var meth = obj[method.name];
         if (meth.apply != null)
            return obj[method.name].apply(obj, paramValues);
         else
            console.log("Error - attempt to invoke non-method: " + method.name);
      }
   }
}

sc_DynUtil_c.invokeRemote = function(obj, method, paramValues) {
   return sc_SyncManager_c.invokeRemote(null, null, obj, null, method.name, method.paramSig, paramValues);
}

sc_DynUtil_c.evalArithmeticExpression = function(operator, expectedType, lhsVal, rhsVal) {
   if (sc_instanceOf(lhsVal, String) || sc_instanceOf(rhsVal, String)) {
      // assert operator = + JS will blow up with an equivalent error
      return lhsVal + rhsVal;
   }
   return eval(lhsVal + operator + rhsVal);
}

sc_DynUtil_c.evalUnaryExpression = function(op, type, val) {
   return eval(op + val);
}

sc_DynUtil_c.evalCast = function(type, val) {
   return val;
}

sc_DynUtil_c.evalPreConditionalExpression = function(op, val) {
   if (op == "&&") {
      if (!val) 
         return false;
   }
   else if (op == "||") {
      if (val)
         return true;
   }
   return null;
}

sc_DynUtil_c.evalConditionalExpression = function(op, lhs, rhs) {
   if (op == "==")
      return lhs == rhs;
   else if (op == "!=")
      return lhs != rhs;
   else if (op == "<")
      return lhs < rhs;
   else if (op == ">")
      return lhs > rhs;
   else if (op == "<=")
      return lhs <= rhs;
   else if (op == ">=")
      return lhs >= rhs;
   else if (op == "&&")
      return lhs && rhs;
   else if (op == "||")
      return lhs || rhs;
   else if (op == "instanceof") {
      // RHS is the "_c" type normally.  We need to compare the constructors.
      return sc_instanceOf(lhs, rhs.constructor);
   }
   else {
      console.log("unrecognized op: " + op);
      return false;
   }
}

sc_DynUtil_c.isEvalSupported = function() { return true; }

sc_DynUtil_c.evalScript = function(script) {
   // Using window.eval here so the var _c classes are defined globally
   // We could also define them with window.sc_xx_c = sc_newClass(...)
   return window.eval(script);
}

sc_DynUtil_c.cleanTypeName = function(tn) {
   return tn;
}

sc_DynUtil_c.getInstanceId = function(obj) {
   if (obj == null)
      return null;
   if (sc_DynUtil_c.isType(obj))
      return sc_DynUtil_c.getTypeName(obj, false);
   if (sc_instanceOf(obj, sc_IObjectId)) {
      return obj.getObjectId();
   }

   if (obj.$objectName !== undefined)
      return obj.$objectName;


   var type = sc_DynUtil_c.getType(obj);

   if (sc_instanceOf(obj, jv_Enum) || obj == type)
      return sc_DynUtil_c.getTypeName(type, false) + "." + sc_DynUtil_c.getEnumName(obj);

   return sc_DynUtil_c.getObjectId(obj, type, null);
}

sc_DynUtil_c.isEnumConstant = function(obj) {
   return sc_instanceOf(obj, jv_Enum);
}

sc_DynUtil_c.isEnumType = function(type) {
   return sc_isAssignableFrom(jv_Enum, obj);
}

sc_DynUtil_c.getObjectId = function(obj, type, typeName) {
   var scid = sc_id(obj);
   var objId = sc$objectIds[scid];
   if (objId !== undefined)
      return objId;

   if (typeName == null) 
      typeName = sc_DynUtil_c.getInstanceId(type);
   var typeIdStr = "__" + sc_DynUtil_c.getTypeIdCount(typeName);
   objId = sc$objectIds[scid] = typeName + typeIdStr;
   return objId;
}

sc_DynUtil_c.setObjectId = function(obj, name) {
   sc$objectIds[sc_id(obj)] = name;
}

sc_DynUtil_c.getTypeIdCount = function(typeName) {
   var typeId = sc$typeIdCounts[typeName];
   if (typeId === undefined) {
      sc$typeIdCounts[typeName] = 1;
      typeId = 0;
   }
   else {
      sc$typeIdCounts[typeName] = 1 + typeId;
   }
   return typeId;
}

sc_DynUtil_c.updateTypeIdCount = function(typeName, ct) {
   sc$typeIdCounts[typeName] = ct;
}

sc_DynUtil_c.isType = sc_DynUtil_c.isSType = function(obj) {
   return obj.hasOwnProperty("$protoName") || obj.hasOwnProperty("$typeName");
}

sc_DynUtil_c.getInstanceName = function(obj) {
   if (obj == null) 
       return "null";

   if (obj.$objectName !== undefined)
      return obj.$objectName;

   if (sc_instanceOf(obj, String))
      return '"' + obj.toString() + '"';

   if (sc_instanceOf(obj, Number))
      return obj.toString();
   
   if (obj.hasOwnProperty("$protoName")) // It's a type - just return the type name
      return obj.$protoName;

   // For objects just return their name since there's one per instance (currently).  Might need to look at the scope for implementing this if JS ever supports non-global scopes.
   var type = sc_DynUtil_c.getType(obj);
   if (type.$objectType)
      return sc_DynUtil_c.getTypeName(type, false);

   if (sc_DynUtil_c.isObject(obj)) {
      var res = sc_DynUtil_c.getObjectName(obj);
      if (res != null)
         return res;
   }

   var scid = sc_id(obj);
   var id = sc$objectIds[scid];
   if (id != null)
      return id;

   var str = obj.toString();
   if (str != null && str.length < 60 && str != "[object Object]")
      return str;

   return sc_CTypeUtil_c.getClassName(obj.$protoName) + '__' + scid;
}

sc_DynUtil_c.arrayToInstanceName = function(list) {
   if (list == null)
      return "";
   var res = new Array();
   for (var i = 0; i < list.length; i++) {
      if (i != 0)
          res.push(", ");
      res.push(sc_DynUtil_c.getInstanceName(list[i]));
   }
   return res.join("");
}

sc_DynUtil_c.getArrayLength = function(arr) {
   if (arr == null)
      console.error("*** No array");
   if (sc_instanceOf(arr, jv_Collection) || sc_instanceOf(arr, jv_Map))
      return arr.size();
   else
      return arr.length;
}

sc_DynUtil_c.getArrayElement = function(arr, ix) {
   if (sc_instanceOf(arr, jv_List))
      return arr.get(ix);
   else
      return arr[ix];
}

sc_DynUtil_c.getIndexedProperty = function(obj, prop, ix) {
   return obj[prop][ix];
}

sc_DynUtil_c.setIndexedProperty = function(obj, prop, ix, val) {
   obj[prop][ix] = val;
}

sc_DynUtil_c.getObjChildren = function(obj, scopeName, create) {
   if (obj.getObjChildren == null)
      return [];
   return obj.getObjChildren(create);
}

sc_DynUtil_c.equalObjects = function(o1, o2) {
   // Need to check typeof for == because it will do a conversion and for example 0 == "" is true
   return (o1 == o2 && typeof o1 == typeof o2) || (o1 != null && (o1.equals != null && o1.equals(o2)));
}

sc_DynUtil_c.equalArrays = function(a1, a2) {
   if (a1 == a2) 
      return true;
   if (a1 == null || a2 == null)
      return false;
   if (a1.length != a2.length)
      return false;
   for (var i = 0; i < a1.length; i++) 
      if (!sc_DynUtil_c.equalObjects(a1[i], a2[i]))
         return false;
   return true;
}

sc_DynUtil_c.toString = function(obj) {
   if (obj == null)
      return "null";
   if (obj instanceof Array) 
      return sc_DynUtil_c.getArrayName(obj);
   return obj.toString();
}

sc_DynUtil_c.getArrayName = function(arr) {
   var res = "{";
   for (var i = 0; i < arr.length; i++) {
      if (i != 0) res = res + ", ";
      res = res + sc_DynUtil_c.toString(arr[i]);
   }
   res += "}";
   return res;
}

sc_DynUtil_c.createInnerInstance = function(newClass, outer, paramSig, paramValues) {
   paramValues = sc_vararg(arguments, 3);
   var DynInst = function(){}; // temporary constructor
   var inst, ret; 

   // True if we are called with the _c value versus the actual constructor function
   var isPrototype = newClass.hasOwnProperty("$protoName");

   // TODO: the name of the constructor we see in the debugger is 'DynInst' 
   // Can we somehow use newClass's constructor here?
   // Give the DynInst constructor the Constructor's prototype
   if (isPrototype)
      DynInst.prototype = newClass.constructor.prototype;
   else
      DynInst.prototype = newClass.prototype;

   // Create a new instance
   inst = new DynInst;

   // For static types, outer is a type here and not a constructor param
   if (outer !== null && !sc_DynUtil_c.isType(outer)) {
      if (paramValues == null)
         paramValues = [outer];
      else {
         paramValues = paramValues.slice();
         paramValues.splice(0, 0, outer);
      }
   }

   // Call the original Constructor with the temp
   // instance as its context (i.e. its 'this' value)

   if (isPrototype) 
      ret = newClass.constructor.apply(inst, paramValues);
   else
      ret = newClass.apply(inst, paramValues);  // we are called with the constructor

   // If an object has been returned then return it otherwise
   // return the original instance.
   // (consistent with behaviour of the new operator)
   return Object(ret) === ret ? ret : inst;
}

sc_DynUtil_c.newInnerInstance = function(type, outer, constrSig, params) {
   params = sc_vararg(arguments, 3);
   if (!sc_DynUtil_c.isComponentType(type)) {
      return sc_DynUtil_c.createInnerInstance(type, outer, constrSig, params);
   }
   var t = outer == null ? type : outer;
   var typeName = sc_DynUtil_c.getTypeName(type);
   var name = sc_CTypeUtil_c.getClassName(typeName);
   var newMeth = t[sc_newName(name)];
   if (!newMeth)
      throw new jv_IllegalArgumentException("No method: " + typeName + ':' + name);
   return newMeth.apply(outer, params);
}


sc_DynUtil_c.createInstance = function(newClass, paramSig, paramValues) {
   return sc_DynUtil_c.createInnerInstance(newClass, null, paramSig, sc_vararg(arguments, 2));
}

// Must be called with the _c object which has the metadata.  We only know
// the property metadata for specific types used in synchronization for which
// we need to create a compatible type on the client using metadata of the type
// on the server (e.g. a collection which supports data binding)
sc_DynUtil_c.getPropertyType = function(type, propName) {
   var res = null;
   if (type._PROPS) {
      var typeName = type._PROPS[propName];
      if (typeName) {
         res = sc_DynUtil_c.findType(typeName);
         if (res != null)
            return res;
      }
   }
   var ext = sc_DynUtil_c.getExtendsType(type);
   if (ext)
      return sc_DynUtil_c.getPropertyType(ext, propName);
   return null;
}

sc_DynUtil_c.hasProperty = function(obj, prop) {
   return obj[prop] !== undefined || obj[sc_getName(prop)] !== undefined;
}

sc_DynUtil_c.getStaticProperty = function(type, prop) {
   var gn = sc_getName(prop);
   var gm = type[gn];
   if (gm !== undefined) {
      // there may be a getMethod but it may have args - the code gen will return undefined in this case so we just ignore the getX method then.
      var res = gm.apply(null);
      if (res !== undefined)
         return res;
   }
   var pval = type[prop];
   if (pval !== undefined)
      return pval;
   return null;
}

// Like getPropertyValue but works for static properties (where obj is the type, not the instance)
sc_DynUtil_c.getProperty = function(obj, prop) {
   if (sc_DynUtil_c.isType(obj))
      return sc_DynUtil_c.getStaticProperty(obj, prop);
   return sc_DynUtil_c.getPropertyValue(obj, prop);
}

sc_DynUtil_c.getPropertyValue = function(obj, prop, ignoreError) {
   var gn = sc_getName(prop);
   var getMethod = obj[gn];
   if (getMethod !== undefined) {
      var res = getMethod.call(obj); 
      if (res !== undefined)
         return res;
   }
   gn = sc_isName(prop);
   getMethod = obj[gn];
   if (getMethod !== undefined) {
      var res = getMethod.call(obj); 
      if (res !== undefined)
         return res;
   }
   var res = obj[prop];
   if (res === undefined) {
      if (arguments.length == 2 || !ignoreError) 
         throw new jv_IllegalArgumentException("Object: " + sc_DynUtil_c.getInstanceId(obj) + " missing property: " + prop);
      else 
         res = null;
   }
   return res;
}

sc_DynUtil_c.setPropertyValue = function(obj, prop, val) {
   var setMethod = obj[sc_setName(prop)];
   if (setMethod !== undefined)
      setMethod.call(obj, val); 
   else
      obj[prop] = val;
}

sc_DynUtil_c.setAndReturn = function(obj, prop, val) {
   sc_DynUtil_c.setPropertyValue(obj, prop, val);
   return val;
}

sc_DynUtil_c.getInnerTypeName = function(type) {
   var typeName = sc_DynUtil_c.getTypeName(type, false);
   if (type.$outerClass == null)
      return sc_CTypeUtil_c.getClassName(typeName);
   return sc_DynUtil_c.getInnerTypeName(type.$outerClass) + "." + sc_CTypeUtil_c.getClassName(typeName);
}

sc_DynUtil_c.getPackageName = function(type) {
   var typeName = sc_DynUtil_c.getTypeName(type, false);
   if (type.$outerClass == null)
      return sc_CTypeUtil_c.getPackageName(typeName);
   return sc_DynUtil_c.getPackageName(type.$outerClass);
}

sc_DynUtil_c.getRootTypeName = function(name, returnLastClass) {
   var prevName = name;
   var any = false;
   for (var startName = sc_CTypeUtil_c.getPackageName(prevName); startName != null; ) {
      // We sometimes omit the types for an intermediate subobject so keep trying the parent even if there's no class for the child.
      if (sc$classTable[startName] !== undefined) {
         if (returnLastClass)
            return startName;
         prevName = startName;
         any = true;
      }
      startName = sc_CTypeUtil_c.getPackageName(startName);
   }
   return !any ? null : prevName;
}

sc_DynUtil_c.getEnclosingType = function(typeObj) {
   if (typeObj.$outerClass == null) // need to convert undefined to null here
      return null;
   return typeObj.$outerClass;
}

sc_DynUtil_c.getExtendsType = function(typeObj) {
   var ext = typeObj.constructor.$extendsClass;
   if (ext != null)
      return ext.prototype;
   return null;
}

sc_DynUtil_c.getPackageName = function(type) {
   return sc_CTypeUtil_c.getPackageName(sc_DynUtil_c.getRootTypeName(sc_DynUtil_c.getTypeName(type, false), false));
}

// TODO: use the annotations so we can return a custom scope for editing purposes on the client
sc_DynUtil_c.getScopeName = sc_DynUtil_c.getScopeNameForType = function(obj) {
   return null;
}

sc_DynUtil_c.getScopeByName = function(name) {
   return sc_ScopeDefinition_c.getScopeByName(name);
}

sc_DynUtil_c.findType = function(name) {
   if (name == null) {
      console.error("Null name passed to findType");
      return null;
   }
   if (name.indexOf('[') != -1) // TODO: Do we need to create a new type for each element type?
      return jv_Array_c;
   var res = sc$classTable[name];
   if (res != null) {
      sc_clInit(res);
      return res.prototype;
   }
   if (name == "int" || name == "float" || name == "double" || name == "long")
      return Number_c;
   if (name == "java.lang.String")
      return String_c;
   return null;
}

sc_DynUtil_c.resolveName = function(name, create) {
   // Here we get the last type for 'name' since inner classes are not properties
   var rootName = sc_DynUtil_c.getRootTypeName(name, true);
   if (rootName != null && name != rootName) {
      var tailName = name.substring(rootName.length + 1);
      var root = sc_DynUtil_c.resolveName(rootName);
      if (root == null)
         return null;

      var cur = root;
      do {
         var propName = sc_CTypeUtil_c.getHeadType(tailName);
         var tailName = sc_CTypeUtil_c.getTailType(tailName);
         if (propName == null) {
            propName = tailName;
            tailName = null;
         }
         sc_clInit(cur);
         var next = sc_DynUtil_c.getPropertyValue(cur, propName, true); // Returns null if not there with this third arg - does not throw
         if (next == null) {
            cur = null;
            break; // Still need to see if this is perhaps a class name.
         }
         cur = next;
      } while (tailName != null);
      if (cur != null)
         return cur;
   }
   // Is this an object registered in the dyn object table?  The table stores
   // all instances so need to verify it's an object before returning it.
   var insts = sc$instancesByType[name];
   if (insts !== undefined) {
      for (var instId in insts) {
         var inst = insts[instId];
         if (inst.$objectType)
            return inst;
      }
   }
   var type = sc$classTable[name];
   if (type != null) {
      type = type.prototype;
      var res = sc_DynUtil_c.getStaticProperty(type, sc_CTypeUtil_c.getClassName(name));
      if (res != null)
         return res;
      return type;
   }
   return null;
}

sc_DynUtil_c.getTypeName = function(cl,includeDims) {
   // TODO: should be passed only the prototype but will it ever be passed the constructor?
   // TODO: not implementing includeDims - not called consistently either
   if (cl.$protoName != null)
      return cl.$protoName;
   if (cl.typeName != null) // This is the TypeDeclaration case 
      return cl.typeName;
   return cl.$typeName;
}

sc_DynUtil_c.getObjectName = function(obj) {
   var objName = obj.$objectName;

   var outer = obj.outer;
   if (objName === undefined) {
      if (outer === undefined) {
         var typeObj = sc_DynUtil_c.getType(obj);
         if (sc_DynUtil_c.isObjectType(typeObj))
            return sc_DynUtil_c.getTypeName(obj);
         return sc_DynUtil_c.getInstanceId(obj);
      }
      else {
         var outerName = sc_DynUtil_c.getObjectName(outer);
         var typeClassName = sc_CTypeUtil_c.getClassName(sc_DynUtil_c.getType(obj).$protoName);
         var objTypeName = outerName + "." + typeClassName;
         if (sc_DynUtil_c.isObjectType(sc_DynUtil_c.getType(obj)))
            return objTypeName;
         if (sc_instanceOf(obj, sc_IObjectId))
            return sc_DynUtil_c.getInstanceId(obj);
         return sc_DynUtil_c.getObjectId(obj, null, objTypeName);
      }
   }

   if (outer === undefined)
       return objName;

   return sc_DynUtil_c.getObjectName(outer) + "." + objName;
}

sc_DynUtil_c.getNumInnerTypeLevels = function(type) {
   var ct = 0;
   //if (obj.$objectName === undefined)
   //    return 0;
   var outer = type.$outerClass;
   if (outer === undefined)
      return 0;
   return sc_DynUtil_c.getNumInnerTypeLevels(outer) + 1;
}

sc_DynUtil_c.getNumInnerObjectLevels = function(obj) {
   var ct = 0;
   //if (obj.$objectName === undefined)
   //    return 0;
   var outer = obj.outer;
   if (outer === undefined)
      return 0;
   return sc_DynUtil_c.getNumInnerObjectLevels(outer) + 1;
}

sc_DynUtil_c.invokeLater = function(runnable, priority) {
   sc_addRunLaterMethod(runnable, runnable.run, priority);
}

sc_DynUtil_c.dispose = function(obj, disposeChildren) {
   if (sc$dynListeners != null) {
      for (var i = 0; i < sc$dynListeners.length; i++)
         sc$dynListeners[i].instanceRemoved(obj);
   }
   sc_SyncManager_c.removeSyncInst(obj);
   sc_Bind_c.removeBindings(obj, false);

   if (sc_instanceOf(obj, sc_IStoppable)) {
       obj.stop();
   }

   if (disposeChildren) {
      var children = sc_DynUtil_c.getObjChildren(obj, null);
      if (children != null) {
         for (var i = 0; i < children.length; i++) {
            sc_DynUtil_c.dispose(children[i]);
         }
      }
   }

   var typeName = sc_DynUtil_c.getTypeName(obj, false);
   var insts = sc$instancesByType[typeName];
   if (insts != null) {
       delete insts[sc_id(obj)];
   }
}

sc_DynUtil_c.isComponentType = function(type) {
   return type._A_Component !== undefined;
}

function sc_IObjChildren() {}

sc_IObjChildren_c = sc_newClass("sc.dyn.IObjChildren", sc_IObjChildren, null, null);

function sc_IStoppable() {}
sc_IStoppable_c = sc_newClass("sc.obj.IStoppable", sc_IStoppable, null, null);

function sc_IComponent() {}

sc_IComponent_c = sc_newClass("sc.obj.IComponent", sc_IComponent, sc_IStoppable, null);

function sc_PTypeUtil() {}

sc_PTypeUtil_c = sc_newClass("sc.type.PTypeUtil", sc_PTypeUtil, null, null);

sc_PTypeUtil_c.isPrimitive = function(type) {
   return false; // TODO: some way to reprent int.class etc.
}

sc_PTypeUtil_c.isANumber = function(type) {
   var tot = typeof type;
   return tot == "number" || (tot == "object" && type.constructor === Number);
}

sc_PTypeUtil_c.isStringOrChar = function(type) {
   var tot = typeof type;
   return tot == "string" || (tot == "object" && type.constructor === String);
}

sc_PTypeUtil_c.getArrayLength = sc_DynUtil_c.getArrayLength;
sc_PTypeUtil_c.getArrayElement = sc_DynUtil_c.getArrayElement;

sc_PTypeUtil_c.postHttpRequest = function(url, postData, contentType, listener) {
   var httpReq = new XMLHttpRequest();
   httpReq.open("POST", url, true);
   httpReq.onload = function(evt) {
      var stat = httpReq.status;
      if (stat == 200) 
         listener.response(httpReq.responseText);
      else {
         if (stat != 205) // This is the sync reset response
            sc_countError();
         // This may be the 'reset' request which is not an error
         //console.error("Non status='200' response to POST: status=" + httpReq.status + ": " + httpReq.statusText + " response: " + httpReq.responseText);
         // Called below in onreadystatechange
         else
            listener.error(stat, httpReq.statusText);
      }
   }
   httpReq.onreadystatechange = function (evt) {
      if (httpReq.readyState == 4) {
         var stat = httpReq.status;
         if(stat != 200 && stat != 205) {
            sc_countError();
            listener.error(httpReq.status, httpReq.statusText);
         }
      }
   }
   /*
   httpReq.onabort = httpReq.onError = function(evt) {
      sc_countError();
      console.error("Aborted response to POST: " + httpReq.status + ": " + httpReq.statusText);
      listener.error(httpReq.status, httpReq.statusText);
   }
   */
   httpReq.setRequestHeader("Content-type", contentType);

   httpReq.send(postData);
}

sc_PTypeUtil_c.addScheduledJob = function(runnable, delay, repeat) {
   return sc_addScheduledJob(runnable, runnable.run, delay, repeat);
}

sc_PTypeUtil_c.cancelScheduledJob = function(handle, repeat) {
   sc_cancelScheduledJob(handle, repeat);
}

sc_PTypeUtil_c.addClientInitJob = function(runnable) {
   sc_addClientInitJob(runnable, runnable.run);
}

sc_PTypeUtil_c.getWindowId = function() {
   if (window["sc_windowId"] != undefined)
      return sc_windowId;
   console.error("No sc_windowId defined - usually this is defined in a block of Javascript from the initial page load.");
   return -1;
}

var sc_threadLocalMap = new Object();
sc_PTypeUtil_c.getThreadLocal = function(key) {
   var val = sc_threadLocalMap[key];
   return val === undefined ? null : val; // Java runtime expects nulls, not undefined.
}

sc_PTypeUtil_c.setThreadLocal = function(key, value) {
   var orig = sc_threadLocalMap[key];
   sc_threadLocalMap[key] = value;
   return orig;
}

sc_DynUtil_c.isArray = sc_PTypeUtil_c.isArray = function(type) {
   if (type === null)
      return false;
   if (type == jv_Array_c || type == Array || type == Array_c)
      return true;
   if (Array.isArray(type))
      console.error("old isArray usage!");
   return false;
}

sc_DynUtil_c.getComponentType = sc_PTypeUtil_c.getComponentType = function(value) {
   return jv_Object_c;
}

sc_PTypeUtil_c.newArray = function(type, size) {
   return new Array(size); // unlike java we don't care about component type 
}

sc_PTypeUtil_c.clone = function(obj) {
   return obj.clone();
}

sc_DynUtil_c.isObject = function(obj) {
   return sc_DynUtil_c.getType(obj).$objectType !== undefined || obj.$objectType !== undefined;
}

sc_DynUtil_c.isObjectType = function(type) {
   return type.$objectType !== undefined || sc_DynUtil_c.getAnnotationValue(type, "sc.obj.TypeSettings", "objectType");
}

sc_DynUtil_c.isRootedObject = function(obj) {
   if (sc_instanceOf(obj, jv_Enum))
      return true;
   if (!sc_DynUtil_c.isObject(obj))
      return false;
   var outer = obj.outer;
   if (outer === undefined)
      return true;
   return sc_DynUtil_c.isRootedObject(outer);
}

sc_DynUtil_c.getOuterObject = function(obj) {
   if (obj.outer === undefined)
      return null;
   return obj.outer;
}

sc_DynUtil_c.getEnumName = function(obj) {
   return obj._name;
}

sc_DynUtil_c.getAnnotationValue = function(typeObj, annotName, valName) {
   var keyName = "_A_" + sc_CTypeUtil_c.getClassName(annotName);
   var val = typeObj[keyName];
   if (val == null)
      return null;
   return val[valName];
}

sc_DynUtil_c.hasAnnotation = function(typeObj, annotName) {
   var keyName = "_A_" + sc_CTypeUtil_c.getClassName(annotName);
   var val = typeObj[keyName];
   if (val === undefined)
      return false;
   return true;
}

sc_DynUtil_c.getPropertyAnnotationValue = function(typeObj, propName, annotName, valName) {
   var keyName = "_PT";
   var annots = typeObj[keyName];
   if (annots != null)
      annots = annots[propName];
   if (annots != null) {
      var annotName = sc_CTypeUtil_c.getClassName(annotName);
      var annotVal = annots[annotName];
      if (annotVal != null) {
         var res = annotVal[valName];
         if (res != null)
            return res;
      }
   }
   var extType = sc_DynUtil_c.getExtendsType(typeObj);
   if (extType != null) {
       return sc_DynUtil_c.getPropertyAnnotationValue(extType, propName, annotName, valName);
   }
   return null;
}

function sc_IDynChildManager() {
}

sc_IDynChildManager_c = sc_newClass("sc.dyn.IDynChildManager", sc_IDynChildManager, null, null);

function sc_propTable(prop) {
   var tab = sc$propNameTable[prop];
   if (tab == null)
      sc$propNameTable[prop] = tab = {cap:sc_capitalizeProperty(prop)};
   return tab;
}

function sc_getName(prop, tab) {
   if (!tab)
      tab = sc_propTable(prop);
   var res = tab.getN;
   if (!res) {
      res = tab.getN = "get" + tab.cap;
   }
   return res;
}

function sc_newName(prop) {
   var tab = sc_propTable(prop);
   var res = tab.newN;
   if (!res) {
      res = tab.newN = "new" + tab.cap;
   }
   return res;
}

function sc_setName(prop) {
   var tab = sc_propTable(prop);
   var res = tab.setN;
   if (!res) {
      res = tab.setN = "set" + tab.cap;
   }
   return res;
}

function sc_isName(prop, tab) {
   if (!tab)
      tab = sc_propTable(prop);
   var res = tab.isN;
   if (!res) {
      res = tab.isN = "is" + tab.cap;
   }
   return res;
}

function sc_capitalizeProperty(prop) {
   if (prop == null || prop.length == 0)
      return prop;
   var zero = prop.charAt(0);
   var zeroUp = zero.toUpperCase();
   if (prop.length > 1) { 
      var first = prop.charAt(1);
      var firstUp = first.toUpperCase();
      // Include letters but exclude digits for which both are the same
      if (first == firstUp && firstUp != first.toLowerCase()) // Weird case for java zMin turns into setzMin 
         return prop;
   }
   return zeroUp + prop.substring(1);
}

sc_DynUtil_c.getDynChildManager = function(type) {
   var dcm = type._dynChildManager;
   if (dcm !== undefined) 
      return dcm;
      
   var className = sc_DynUtil_c.getAnnotationValue(type, "sc.obj.CompilerSettings", "dynChildManager");
   if (className != null) {
      var dcmType = sc_DynUtil_c.findType(className);
      if (dcmType == null)
         console.error("No dynChildManager class: " + className);
      else {
         dcm = sc_DynUtil_c.createInstance(dcmType, null, null);
         type._dynChildManager = dcm;
         return dcm;
      }
   }
}

sc_DynUtil_c.addChild = function(arg0, arg1, arg2) {
   var parent, child;
   if (arguments.length == 3) {
      parent = arg1;
      child = arg2;
   }
   else {
      parent = arg0;
      child = arg1;
   }
   var parentType = sc_DynUtil_c.getType(parent);
   var mgr = sc_DynUtil_c.getDynChildManager(parentType);
   if (mgr == null)
      console.error("No dynChildManager for addChild operation on: " + sc_DynUtil_c.getTypeName(parentType, false));
   else {
      if (arguments.length == 3)
         mgr.addChild(arg0, parent, child);
      else
         mgr.addChild(parent, child);
   }
}

sc_DynUtil_c.removeChild = function(parent, child) {
   var parentType = sc_DynUtil_c.getType(parent);
   var mgr = sc_DynUtil_c.getDynChildManager(parentType);
   if (mgr == null)
      console.error("No dynChildManager for removeChild operation on: " + sc_DynUtil_c.getTypeName(parentType, false));
   else {
      mgr.removeChild(parent, child);
   }
}

sc_DynUtil_c.getInstancesOfTypeAndSubTypes = function(typeName) {
   var instObj = sc$instancesByType[typeName];
   var insts;
   if (instObj === undefined)
      insts = null;
   else {
      insts = [];
      for (var instId in instObj) {
         if (instObj.hasOwnProperty(instId)) {
            insts.push(instObj[instId]);
         }
      }
   }
   var type = sc_DynUtil_c.findType(typeName);
   if (type == null) {
      return null;
   }
   var subClasses = type.constructor.$subClasses;
   if (subClasses) {
      for (var i = 0; i < subClasses.length; i++) {
         var subClass = subClasses[i];
         var subInsts = sc_DynUtil_c.getInstancesOfTypeAndSubTypes(subClass.$typeName);
         if (subInsts != null) {
            if (insts == null)
               insts = subInsts;
            else
               insts = insts.concat(subInsts);
         }
      }
   }
   return insts;
}

sc_DynUtil_c.updateInstances = function(typeName) {
   var insts = sc_DynUtil_c.getInstancesOfTypeAndSubTypes(typeName);
   if (insts != null) {
      for (var id in insts) {
         if (insts.hasOwnProperty(id)) {
             var inst = insts[id];
             if (inst._updateInst === undefined) {
                console.error("No _updateInst method registered for: " + typeName);
             }
             else
                 inst._updateInst();
         }
      }
   }
}

sc_DynUtil_c.applySyncLayer = function(lang, dest, scope, layerDef, isReset, allowCodeEval) {
   if (lang.equals("js") && allowCodeEval)
      sc_DynUtil_c.evalScript(layerDef);
   else
      throw new IllegalArgumentException("Error - unable to apply sync layer for lang: " + lang);
}

sc_DynUtil_c.isAssignableFrom = function(s, d) {
   return sc_isAssignableFrom(s.constructor, d.constructor);
}

sc_DynUtil_c.hasPendingJobs = function() {
   return sc_hasPendingJobs();
}

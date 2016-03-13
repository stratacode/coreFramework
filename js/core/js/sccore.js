function sc_countError() {
   if (window.sc_errorCount === undefined)
      window.sc_errorCount = 0;
   window.sc_errorCount++;
   if (window.sc_errorCountListener != undefined)
      window.sc_errorCountListener();
}

// So we can track javascript system errors using the same status symbol
window.onerror = function (errorMsg, url, lineNumber) {
   sc_countError();
}

var sc$classTable = {};
var sc$liveDynamicTypes = true;

var sc$nextid = 1;
function sc_id(obj) {
   if (!sc_hasProp(obj, "sc$id"))
      obj.sc$id = sc$nextid++;
   return obj.sc$id;
}

function sc_hasProp(obj, prop) {
   if (obj.hasOwnProperty)
      return obj.hasOwnProperty(prop);
   return obj[prop] !== undefined;
}

function sc_newObj(typeName, newConstr, extendsClass, implements) {
   return sc_newInnerObj(typeName, newConstr, null, extendsClass, implements);
}

function sc_newInnerObj(typeName, newConstr, outerClass, extendsClass, implements) {
   var proto = sc_newInnerClass(typeName, newConstr, outerClass, extendsClass, implements);
   proto.$objectType = true;
   return proto;
}

function sc_newClass(typeName, newConstr, extendsClass, implements) {
   return sc_newInnerClass(typeName, newConstr, null, extendsClass, implements);
}

function sc_newInnerClass(typeName, newConstr, outerClass, extendsClass, implements) {
   if (sc$classTable[typeName])
      console.log("redefining class with type name: " + typeName);  

   function $clProto() {}; 

   if (newConstr === undefined || newConstr == null) {
      console.log("No constructor for: " + typeName);  
      newConstr = new $clProto();
   }

   newConstr.$typeName = typeName;
   sc$classTable[typeName] = newConstr;
   if (extendsClass) {
      if (extendsClass.$typeName == null)
         console.log("Class: " + typeName + " initialized before its extends class: " + extendsClass);

      $clProto.prototype = extendsClass.prototype;
      newConstr.prototype = new $clProto();
      if (sc$liveDynamicTypes) {
         var subClasses = extendsClass.$subClasses;
         if (subClasses === undefined)
             subClasses = extendsClass.$subClasses = [];
         subClasses.push(newConstr);
      }
   }
   var newProto = newConstr.prototype;
   newProto.$protoName = typeName;

   if (implements) {
      newConstr.$implements = implements;
      /* Currently the interfaces is just a marker on the type and does not actually modify the type.  
      var key; 
      var newProto = newConstr.prototype;

      for (var i = 0; i < implements.length; i++) {
         var impl = implements[i];
	 for (key in impl) {
            if (!newProto.hasOwnProperty(key) && !newConstr.hasOwnProperty(key))
	       newProto[key] = impl[key];
	 }
      }
      */
   }

   newConstr.$extendsClass = extendsClass;
   if (outerClass != null)
      newProto.$outerClass = outerClass.prototype;
   newProto.constructor = newConstr;
   return newProto;
}

function sc_newArray(arrayClass) {
   return sc_initArray(null, arrayClass, arguments, 0);
}

function sc_initArray(array, arrayClass, args, dim) {
   var len = Math.floor(args[1 + dim]);  // skipping original arrayClass arg - len converted to an int just in case it needs to be...
   var ndim = args.length - 2 - dim;
   if (dim == 0) {
      var res = new Array(len);
      res._class = arrayClass;
      res._ndim = ndim;
      array = res;
      for (var j = 0; j < len; j++) {
         if (arrayClass == Number_c)
            array[j] = 0;
         else
            array[j] = null; 
      }
   }
   else if (ndim > 0) {
      for (var i = 0; i < len; i++) {
         array[i] = sc_initArray(array, arrayClass, args, ++dim);
      }
   }
   return array;
}

function sc_isAssignableFrom(srcClass, dstClass) {
   if (dstClass === jv_Object)
      return true;
   if (srcClass === dstClass)
      return true;
   do {
      if (srcClass.prototype === dstClass.prototype) {
         return true;
      }
      if (srcClass.$implements) {
	  for (var i=0; i < srcClass.$implements.length; i++) {
              var impl = srcClass.$implements[i];
              if (sc_isAssignableFrom(impl, dstClass))
		  return true;
          }
      }
      if (srcClass.$extendsClass != null)
         srcClass = srcClass.$extendsClass;
      else
         srcClass = null;
   } while (srcClass);
   return false;
}

function sc_paramType(srcObj, dstClass) {
   if (srcObj == null)
      return true;
   else 
      return sc_instanceOf(srcObj, dstClass);
}

function sc_instanceOf(srcObj, dstClass) {
   if (srcObj == null)
      return false;

   return sc_isAssignableFrom(srcObj.constructor, dstClass);
}

function sc_arrayInstanceOf(srcObj, dstClass, ndim) {
  return srcObj._class != null && srcObj._ndim == ndim && sc_isAssignableFrom(srcObj._class, dstClass);
}

function sc_clInit(c) {
   if (c._clInit != null)
      c._clInit();
   return c;
}

// support either JS Array or List
function sc_length(arr) {
   if (arr === null || arr === undefined)
      return 0;
   if (arr.length)
      return arr.length;
   if (arr.size)
      return arr.size();
   console.log("invalid array");
   return -1;
}

function sc_arrayValue(arr, ix) {
   if (arr.get)
      return arr.get(ix);
   return arr[ix];
}

// Cross browser add and remove event facilities (from John Resig) via the quirksmode.org competition
function sc_addEventListener( obj, type, fn ) {
  if ( obj.attachEvent ) {
    obj['e'+type+fn] = fn;
    obj[type+fn] = function(){obj['e'+type+fn]( window.event );}
    obj.attachEvent( 'on'+type, obj[type+fn] );
  } else
    obj.addEventListener( type, fn, false );
}
function sc_removeEventListener( obj, type, fn ) {
  if ( obj.detachEvent ) {
    obj.detachEvent( 'on'+type, obj[type+fn] );
    obj[type+fn] = null;
  } else
    obj.removeEventListener( type, fn, false );
}

// Returns an event listener which calls the supplied function with (event, arg)
function sc_newEventArgListener(fn, arg) {
   return function(event) {
      var _arg = arg;
      var _fn = fn;
      _fn(event, _arg);
   }
}

function sc_methodCallback(thisObj, method) {
    return function() {
      var _this = thisObj;
      method.call(_this);
   };
}

function sc_methodArgCallback(thisObj, method, arg) {
    return function() {
      var _this = thisObj;
      var _arg = arg;
      method.call(_this, _arg);
   };
}

var sc_runLaterMethods = [];

function sc_runRunLaterMethods() {
   try {
      for (var i = 0; i < sc_runLaterMethods.length; i++) {
         var rlm = sc_runLaterMethods[i];
         rlm.method.call(rlm.thisObj);
      }
   }
   finally {
      sc_runLaterMethods = [];
   }
   sc_runLaterScheduled = false;
}

// Override this to delay when runLaters are called - perhaps till right before refresh
var sc_runLaterScheduled = false;

function sc_addRunLaterMethod(thisObj, method, priority) {
   var i;
   var len = sc_runLaterMethods.length;
   if (len == 0 && !sc_runLaterScheduled) {
      setTimeout(sc_runRunLaterMethods, 1);
      sc_runLaterScheduled = true;
   }
   for (i = 0; i < len; i++) {
      if (priority > sc_runLaterMethods[i].priority)
         break;
   }
   var newEnt = {thisObj:thisObj, method: method, priority:priority};
   if (i == len)
      sc_runLaterMethods.push(newEnt);
   else
      sc_runLaterMethods.splice(i, 0, newEnt);
}

function sc_setMethodTimer(thisObj, method, timeInMillis) {
   return setTimeout(sc_methodCallback(thisObj, method),timeInMillis);
}

function sc_addLoadMethodListener(thisObj, method) {
   if (document.readyState === "complete" ) {
      sc_setMethodTimer(thisObj, method, 1);
   } 
   else if (document.addEventListener) {
      window.addEventListener("load", sc_methodCallback(thisObj, method), false);
   } 
   else {
      window.attachEvent("onload", sc_methodCallback(thisObj, method));
   }
}

// When the last argument is an array, Java will make that be the varArgs so this function simulates that rule at runtime
function sc_vararg(args, ix) {
   if (ix >= args.length) // No parameters at all in the varargs slot
      return [];
   
   var arg = args[ix];

   if (ix == args.length - 1 && arg !== null && arg.constructor === Array)
      return arg;
   return Array.prototype.slice.call(args, ix);
}

// Java does charToInt automatically but for JS this call is inserted by the code-generator
function sc_charToInt(cs) {
   // Assert cs.length == 1 
   return cs.charCodeAt(0);
}


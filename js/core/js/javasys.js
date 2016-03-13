var Error_c = sc_newClass("Error", Error, null, null);

var Number_c = sc_newClass("Number", Number, null, null);

Number_c.TYPE = Number_c;

Number.prototype.hashCode = function() {
   return Math.floor(this) * 17;
}

Number.prototype.intValue = function() {
   return this < 0 ? Math.ceil(this) : Math.floor(this);
}

Number_c.parseInt = parseInt;

Number_c.valueOf = function(v) {
   // build in js method for comparison against a boolean or something
   if (arguments.length == 0)
      return this;

   if (sc_instanceOf(v, Number))
      return v;
   var str = v.toString();
   var res = parseFloat(str); 
   if (isNaN(res))
      throw new jv_NumberFormatException(str + " is not a number");
   return res;
}

Number_c.compare = function(a,b) {
   if (a == b)
      return 0;
   else if (a > b) 
      return 1;
   else
      return -1;
}

Number_c.toHexString = function(v) {
   return v.toString(16);
}

Object.prototype.hashCode = function() {
   return sc_id(this);
}

Object.prototype.equals = function(other) {
   return other === this;
}

// Need some way to get to the original Object's toString method so we have a reasonable default
//Object.prototype.toString = function() {
//   return sc_DynUtil_c.getInstanceName(this);
//}

Object.prototype.getClass = function() {
   return this.constructor.prototype;
}

Object.prototype.clone = function() {
   // TODO: will this get called for Node's the DOM?  If so should we call cloneNode?

   // Technically should throw here if this is not an instanceof Cloneable but that behavior is annoying anyway.
   var CloneInst = function(){}; // temporary constructor
   var clone;
   CloneInst.prototype = this.constructor.prototype;

   // Create a new instance using the classes prototype
   clone = new CloneInst;

   // Just copy over the properties - similar to how Java works.  It does not call the constructor
   for (var prop in this) {
      if (this.hasOwnProperty(prop)) 
         clone[prop] = this[prop];
   }
/*
   var paramValues;
   if (this.outer !== undefined)
      paramValues = [outer];
   else
      paramValues = [];
*/

   // Call the constructor
   //ret = this.constructor.apply(clone, paramValues);
   // If the constructor returned an instance use that, otherwise use the orig
   //clone = Object(ret) === ret ? ret : clone;

   return clone;
}

var String_c = sc_newClass("String", String, null, null);

String_c._valueOf = function(o) {
   return o === null || o === undefined ? "null" : o.toString();
}

String.prototype._length = function() {
   return this.length;
}

String.prototype.contains = function(arg) {
   return this.indexOf(arg) != -1;
}

String.prototype.hashCode = function() {
   var len = this.length;
   return len == 0 ? 0 : len == 1 ? this.length * this.charCodeAt(0) * 53 : this.length * this.charCodeAt(0) * this.charCodeAt(len-1) * 7;
}

String.prototype.toString = function() {
   return this;
}

String.prototype.startsWith = function(str) {
   return this.indexOf(str) == 0;
}

String.prototype.endsWith = function(str) {
   var idx = this.lastIndexOf(str);
   return idx != -1 && this.length - str.length == idx;
}

String.prototype.equals = function(other) {
   if (other === null)
      return false;
   return this.length == other.length && this.compareTo(other) == 0;
}

String.prototype.compareTo = function(other) {
   var len1 = this.length;
   var len2 = other.length;
   var num = Math.min(len1, len2);
   for (var i = 0; i < num; i++) {
      var c1 = this.charAt(i);
      var c2 = other.charAt(i);
      if (c1 != c2)
         return c1 - c2;
   }
   return len1 - len2;
}

function jv_Object() {
}

var jv_Object_c = sc_newClass("jv_Object", jv_Object, null, null);

jv_Object_c.equals = function(other) {
   return this == other;
}

jv_Object_c.getClass = function() {
   return this.constructor.prototype;
}

// All class objects have a getName method but our only way to add methods to java.lang.Class is via jv_Object here.  
jv_Object_c.getName = function() {
   if (this.hasOwnProperty("$protoName"))
      return this.$protoName;
   throw new UnsupportedOperationException();
}

var Boolean_c = sc_newClass("Boolean", Boolean, null, null);

Boolean_c.FALSE = false;
Boolean_c.TRUE = true;

function jv_Void() {}
var jv_Void_c = sc_newClass("jv_Void", jv_Void, null, null);

function jv_Byte() {}
var jv_Byte_c = sc_newClass("jv_Byte", jv_Byte, null, null);

Error.prototype.printStackTrace = function() {
  if (this.stack)
     console.log(this.stack);
}

function jv_System() {
}

var jv_System_c = sc_newClass("jv_System", jv_System, null, null);

jv_System_c.out = new jv_PrintStream();
jv_System_c.err = new jv_PrintStream(true);

jv_System_c.arraycopy = function(src, srcPos, dst, dstPos, length) {
   if (srcPos < dstPos) {
      for (var i = length - 1; i >= 0; i--) {
         dst[dstPos + i] = src[srcPos + i];
      }
   }
   else {
      for (var i = 0; i < length; i++) {
         dst[dstPos + i] = src[srcPos + i];
      }
   }
}

jv_System_c.currentTimeMillis = function() {
   if (Date.now) 
     return Date.now();
   return new Date().getTime();
}

jv_System_c.identityHashCode = function(obj) {
   if (obj == null)
      return 0;
   return sc_id(obj);
}

function jv_PrintStream() {
   this.buf = null;
   this.err = arguments.length == 1 && arguments[0];
}

var jv_PrintStream_c = sc_newClass("jv_PrintStream", jv_PrintStream, null, null);

jv_PrintStream_c.println = function() {
   var res = "";
   for (var i = 0; i < arguments.length; i++) {
      res = res + arguments[i];
   }
   var buf = this.buf;
   var str = ((buf == null ? "" : buf) + res);
   if (this.err && console.error) {
      console.error(str);
      sc_countError();
   }
   else if (console.log) 
      console.log(str);
   this.buf = null;
}

jv_PrintStream_c.print = function() {
   var res = "";
   for (var i = 0; i < arguments.length; i++) {
      res = res + arguments[i];
   }
   // No way to print without a newline so buffer this stuff up.
   if (this.buf == null)
      this.buf = res;
   else
      this.buf = this.buf + res;
}

function jv_Enum(name, ord) {
   this._name = name;
   this._ordinal = ord;
}

jv_Enum_c = sc_newClass("jv_Enum", jv_Enum);

jv_Enum_c.toString = function() {
   return this._name;
}

jv_Enum_c.values = jv_Enum_c.getEnumConstants = function() {
   sc_clInit(this);
   if (this._values === undefined)
      console.log("*** enum not initialized");
   return this._values;
}

jv_Enum_c.valueOf = function(name) {
   sc_clInit(this);
   for (var i = 0; i < this._values.length; i++) 
      if (this._values[i]._name == name)
        return this._values[i];
   return null;
}

function jv_StringBuilder() {
  this.value = new Array();
}


jv_StringBuilder_c = sc_newClass("jv_StringBuilder", jv_StringBuilder);

jv_StringBuilder_c.append = function(v) {
   if (v != null)
      this.value.push(v.toString());
}

jv_StringBuilder_c._length = function() {
   var len = 0;
   for (var i = 0; i < this.value.length; i++) 
      len += this.value[i].length;
   return len;
}

jv_StringBuilder_c.toString = function() {
   return this.value.join("");
}

Array.prototype.clone = function() {
   var sz = this.length;
   var newArray = new Array(sz);
   var len = newArray.length;
   for (var i = 0; i < sz; i++) {
      newArray[i] = this[i];
   }
   return newArray;
}

function jv_Array() {
}

Array_c = sc_newClass("Array", Array);

jv_Array_c = sc_newClass("jv_Array", jv_Array, Array, null);

jv_Array_c.newInstance = function() {
   var res = null;
   // Ignoring arguments[0] - the type
   for (var i = 1; i < arguments.length; i++) {
      var nextDim = new Array(arguments[i]); 
      if (res == null)
         res = nextDim;
      else
         res = res[nextDim];
   }
   return res;
}

function jv_Exception() {
   // TODO: simulate this on IE using callee and arguments perhaps?
   this.stack = new Error().stack;
}

jv_Exception_c = sc_newClass("jv_Exception", jv_Exception);

jv_Exception_c.printStackTrace = function() {
   console.log(this.stack);
}

var jv_enableAsserts = true;

function jv_assert(expr) {
   if (!expr && jv_enableAsserts) {
      console.log("Assertion failed" + (arguments.length > 1 ? (": " + arguments[1]) : ""));
   }
}

function jv_Character() {
}

jv_Character_c = sc_newClass("jv_Character", jv_Character);

jv_Character_c.isLowerCase = function(c) {
   return c.isLowerCase();
}

jv_Character_c.isUpperCase = function(c) {
   return c.isUpperCase();
}

function jv_Math() {
}

jv_Math_c = sc_newClass("jv_Math", jv_Math);

jv_Math_c.max = function(arg1,arg2) {
   return Math.max(arg1, arg2);
}

jv_Math_c.min = function(arg1,arg2) {
   return Math.min(arg1, arg2);
}

jv_Math_c.abs = function(arg) {
   return Math.abs(arg);
}

jv_Math_c.floor = function(arg) {
   return Math.floor(arg);
}

jv_Math_c.round = function(arg) {
   return Math.round(arg);
}

// These are methods we use to implement the java.lang.Class methods - equals, hashCode, getName, etc.  
// Because JS has a single name space - shared by both types and instances - the generated code will redirect
// to these methods when 'this = <the class>'.
jv_Class_c = {};

jv_Class_c.equals = jv_Object_c.equals;
jv_Class_c.hashCode = jv_Object_c.hashCode;
jv_Class_c.getName = function() {
   return this.$protoName;
}

jv_Class_c.toString = function() {
   return this.$protoName;
}

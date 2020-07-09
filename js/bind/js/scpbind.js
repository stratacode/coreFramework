
function sc_PBindUtil() {}

sc_PBindUtil_c = sc_newClass("sc_PBindUtil", sc_PBindUtil, null, null);

sc_PBindUtil_c.trackAllObjects = true;
sc_PBindUtil_c.allObjects = {};

sc_PBindUtil_c.getPropertyValue = function(obj, prop) {
   if (sc_instanceOf(prop, sc_IListener))
      return prop.getPropertyValue(obj);
   if (sc_DynUtil_c.isType(obj))
      sc_clInit(obj);
   return sc_DynUtil_c.getPropertyValue(obj, prop);
}

sc_PBindUtil_c.setPropertyValue = function(obj, prop, val) {
   if (sc_instanceOf(prop, sc_IListener))
      prop.setPropertyValue(obj, val);
   else 
      sc_DynUtil_c.setPropertyValue(obj, prop, val);
}

sc_PBindUtil_c.setIndexedProperty = function(obj, prop, ix, val) {
   obj[prop][ix] = val;
   sc_Bind_c.sendEvent(sc_IListener_c.ARRAY_ELEMENT_CHANGED, obj, prop, ix);
}

sc_PBindUtil_c.addBindingListener = function(obj, prop, listener, event) {
   if (sc_instanceOf(prop, sc_IListener))
      prop.addBindingListener(obj, listener, event);
   else
      sc_Bind_c.addListener(obj, prop, listener, event);
}

sc_PBindUtil_c.removeBindingListener = function(obj, prop, listener, event) {
   if (sc_instanceOf(prop, sc_IListener))
      prop.removeBindingListener(obj, listener, event);
   else
      sc_Bind_c.removeListener(obj, prop, listener, event);
}

sc_PBindUtil_c.getBindings = function(obj) {
   if (obj == null || obj === undefined)
      console.log("*** invalid call to getBindings");
   // Don't inherit this from the prototype because we use it on the prototype to manage static bindings
   if (!sc_hasProp(obj, "_bindings"))
      return null;
   return obj._bindings;
}

sc_PBindUtil_c.printAllBindings = function() {
   if (!sc_PBindUtil_c.trackAllObjects)
      console.log("Error: trackAllObjects disabled when printAllBindings is called");
   var allObjs = sc_PBindUtil_c.allObjects;
   for (var objId in allObjs) {
      if (allObjs.hasOwnProperty(objId)) {
         sc_Bind_c.printBindings(allObjs[objId]);
      }
   }
}

sc_PBindUtil_c.setBindings = function(obj, bindings) {
   obj._bindings = bindings;
}

sc_PBindUtil_c.addListener = function(obj, prop, listener, eventMask, priority) {
   if (obj === null || obj === undefined) {
      console.error("Invalid call to addListener for null/undefined object and property: " + prop);
      return;
   }
      
   var bls = sc_getBindListeners(obj);
   var newEntry = {listener: listener, eventMask: eventMask, priority: priority, flags: (listener.getVerbose() ? sc_Bind_c.VERBOSE : 0) | (listener.getTrace() ? sc_Bind_c.TRACE : 0)};
   if (bls == null) {
      obj._bindingListeners = bls = new Object();
      if (sc_PBindUtil_c.trackAllObjects)
          sc_PBindUtil_c.allObjects[sc_id(obj)] = obj;
   }
   
   var plist = bls[prop];
   if (plist == null)
      bls[prop] = [newEntry];
   else {
      var lastFreeIx = -1;
      for (var i = 0; i < plist.length; i++) {
         var pent = plist[i];
         if (!pent) {
            lastFreeIx = i;
         }
         // If the new entry is higher priority, we need to put newEntry before pent
         else if (pent.priority < priority)
            break;
         // Otherwise, add this guy in the first free slot (if any) with the same priority
         else if (pent.priority == priority) {
            if (lastFreeIx != -1)
               break;
         }
         else
            lastFreeIx = -1; // Any free slot is too high priority for this new one
      }
      if (lastFreeIx != -1)
         plist[lastFreeIx] = newEntry;
      else
         plist.splice(i, 0, newEntry);

      // TODO: performance - add this to the Listener as a method?
      plist.flags |= (listener.getVerbose() ? sc_Bind_c.VERBOSE : 0) | (listener.getTrace() ? sc_Bind_c.TRACE : 0);
   }
   plist = bls[null]; // "null" key represents listeners on the object itself - ok since null is a keyword and not a valid property name.

   // Check for any listeners on the default property.  If so, notify them (unless this is the LISTENER_ADDED mask only).
   // This is used so any systems listeners, like an on-click handler can be added only when the client is listening for them.
   if (plist != null && ((eventMask & ~sc_IListener_c.LISTENER_ADDED) !== 0)) {
      for (var i = 0; i < plist.length; i++) {
         var defaultEntry = plist[i];
         if (defaultEntry && (defaultEntry.eventMask & sc_IListener_c.LISTENER_ADDED) != 0) {
            defaultEntry.listener.listenerAdded(obj, prop, listener, eventMask, priority);
         }
      }
   }
}

sc_PBindUtil_c.removeStatefulListener = function(obj) {
   if (sc_instanceOf(obj, sc_IBinding))
       obj.removeListener();
}

sc_PBindUtil_c.removeListener = function(obj, prop, listener, eventMask) {
   var bls = sc_getBindListeners(obj);
   if (bls == null) {
      console.log("no listener to remove");
      return;
   }
   var plist = bls[prop];
   if (plist == null) {
      console.log("no listener to remove");
      return;
   }

   var i;
   var len = plist.length;
   var anyLeft = false;
   var found = false;
   var freeAtEnd = 0;
   for (var i = len - 1; i >= 0; i--) { // from back to front to match removeBindings order to minimize search and splice time
      var ent = plist[i];
      if (ent == null) {
         freeAtEnd++;
         continue;
      }
      if (ent.listener === listener && ent.eventMask == eventMask) {
         plist[i] = null;
         found = true;
         freeAtEnd++;
      }
      else {
         freeAtEnd = 0;
         anyLeft = true;
      }
      if (found && anyLeft) {
         if (i < len - 1)
            freeAtEnd = 0;
         break;
      }
   }
   if (i == len)
      console.log("no listener to remove");
   else if (!anyLeft) {
      delete bls[prop];
      if (Object.keys(bls).length == 0) {
         if (sc_PBindUtil_c.trackAllObjects)
            delete sc_PBindUtil_c.allObjects[sc_id(obj)];
      }
   }
   else if (freeAtEnd > 8) {
      plist.splice(plist.length - freeAtEnd, freeAtEnd, 0, 0);
   }
}

sc_PBindUtil_c.sendEvent = function(event, obj, prop, detail) {
   var listeners = sc_PBindUtil_c.getBindingListeners(obj, prop);
   if (listeners != null) {
      // When the event is "value changed", we want to first invalidate all of the listeners, then validated them all
      // Otherwise, we will validate more bindings overall and some of those validations occur using stale values (because we did not
      // notify other bindings).  Maybe there's a way to avoid the two pass by better sorting of bindings (i.e. do other bindings as higher priority than
      // the final assignment?) but I think the two-pass way is the most efficient ultimately.
      if ((event & sc_IListener_c.VALUE_CHANGED) == sc_IListener_c.VALUE_CHANGED) {
         sc_PBindUtil_c.dispatchListeners(listeners, sc_IListener_c.VALUE_INVALIDATED, obj, prop, detail);
         sc_PBindUtil_c.dispatchListeners(listeners, sc_IListener_c.VALUE_VALIDATED, obj, prop, detail);
         event = event & ~(sc_IListener_c.VALUE_CHANGED);
      }
      if (event != 0)
         sc_PBindUtil_c.dispatchListeners(listeners, event, obj, prop, detail);
   }
}

sc_PBindUtil_c.dispatchListeners = function(listeners, event, obj, prop, detail) {
   for (var i = 0; i < listeners.length; i++) {
      var listener = listeners[i];
      if (!listener)
         continue;
      var mask = listener.eventMask & event;
      if (mask != 0)
         sc_Bind_c.dispatchEvent(mask, obj, prop, listener.listener, detail);
   }
}

sc_PBindUtil_c.sendAllEvents = function(event, obj) {
   if (bls == null) 
      return;

   for (var prop in bls) {
      var plist = bls[prop];
      if (plist != null) {
         for (var i = 0; i < plist.length; i++) {
            var listener = plist[i];
            if (listener)
               sc_Bind_c.dispatchEvent(listener.eventMask, obj, prop, listener.listener, detail);
         }
      }
   }
}

sc_PBindUtil_c.getBindingListeners = function(obj, prop) {
   var bls = sc_getBindListeners(obj);
   if (bls == null)
       return null;
   return bls[prop];
}

function sc_getBindListeners(obj) {
   if (obj === undefined || !obj.hasOwnProperty) {
      console.error("*** invalid call to getBindListeners");
      return null;
   }
   if (!obj.hasOwnProperty("_bindingListeners"))
       return null;
   return obj._bindingListeners;
}

sc_PBindUtil_c.printBindingListeners = function(obj) {
   var bls = sc_getBindListeners(obj);
   if (bls == null)
       return;
   
   for (var prop in bls) {
      if (bls.hasOwnProperty(prop)) {
         var plist = bls[prop];
         if (plist != null) {
            console.log("  " + prop);
            for (var i = 0; i < plist.length; i++) {
               var l = plist[i];
               if (!l) continue;
               var listener = sc_Bind_c.getRootListener(l.listener);
               var toStr = listener.toString();
               if (toStr == "[object Object]")
                  toStr = listener.$protoName;
               console.log("     " + toStr);
            }
         }
      }
   }
}

sc_PBindUtil_c.equalProps = function(p1, p2) {
   return p1 == p2;
}

sc_PBindUtil_c.getReverseMethodMapper = function(methBinding) {
   sc_clInit(methBinding.method.type);
   return methBinding.method.type["_" + methBinding.method.name + "MethBindSettings"]; 
}

sc_PBindUtil_c.invokeReverseMethod = function(methBinding, methBindSettings, obj, value, paramValues) {
   var reverseVal = null;
   if (methBindSettings.oneParamReverse) {
      reverseVal = methBindSettings.reverseMethod.call(obj, value);
   }
   else {
      var vlen = paramValues.length + 1;
      var values = new Array(vlen);
      var j = 0;

      for (var i = 0; i < vlen; i++) {
         if (i == methBindSettings.reverseSlot)
            values[i] = value;
         else {
            if (j >= paramValues.length)
               values[i] = null;
            else
               values[i] = paramValues[j++];
         }
      }
      if (obj != null || methBindSettings.reverseMethodStatic) {
         if (methBindSettings.forwardSlot != -1) {
            methBindSettings.reverseMethod.apply(obj, values);
            reverseVal = paramValues[methBindSettings.forwardSlot];
         }
         else
            reverseVal = methBindSettings.reverseMethod.apply(obj, values);
      }
      else
         reverseVal = null;
   }

   var reverseSlot = methBindSettings.reverseSlot;
   var reverseParam = reverseSlot != -1 && methBindSettings.modifyParam ? methBinding.boundParams[reverseSlot] : null;
   if (reverseParam != null && reverseParam.isConstant())
       reverseParam - null;

   if (reverseParam != null) {
      reverseParam.applyReverseBinding(null, reverseVal, methBinding);
   }
}

sc_PBindUtil_c.propagateReverse = function(methBindSettings, ix) {
   return methBindSettings.modifyParam && ix == methBindSettings.reverseSlot;
}

// In JS land today all properties are passed around as string names.
sc_PBindUtil_c.getPropertyName = function(prop) {
   return prop;
}

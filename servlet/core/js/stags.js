/**
* stags.js: implements 'serverTags only' Javascript support.
* This is a lighter version of tags.js apis and features, some overlapping code + structure.  Used when there is no Java code on the client for an schtml server page.
* Because there's no Java code converted to JS, this page has less to do than tags.js and eliminates many significant dependencies (e.g. javasys.js).
* It uses the same 'sync' protocol as the full JS runtime, including creating a list of local tag objects representing the 'serverTags' list received from the server.
* That includes the list of DOM elements with event listeners added on the server - i.e. the events we need to listen to on the client.
* For each server tag, we'll create a corresponding tagObject and add listeners for those DOM events.  When the events fire, a change
* to the synchronized state is queued up and 'doLater' to sync the change and receive the response set of changes from the server.
* Those can include changes to the serverTags list or updates to the DOM elements.
* Just like tags.js, tagObjects wrap DOM elements to shadow the state from the server
*/
function sc_newClass(typeName, newConstr, extendsConstr) {
   if (extendsConstr)
      newConstr.prototype = Object.create(extendsConstr.prototype);
   newConstr.prototype.$typeName = typeName;
   return newConstr.prototype;
}
var sc$propNameTable = {};
var sc$nextid = 1;
var sc$dynStyles = {};
var sc$resetState = {}; // any sync'd state that does not correspond to the DOM is stored here - to be sent back when we need to reset the server's session
var sc$startTime = new Date().getTime();
function sc_id(o) {
   if (!sc_hasProp(o, "sc$id"))
      o.sc$id = sc$nextid++;
   return o.sc$id;
}
function sc_instId(o) {
   if (o.getId)
      return o.getId();
   return o.$typeName + "_" + sc_id(o);
}

// Cross browser add and remove event facilities (from John Resig) via the quirksmode.org competition
// TODO: is this necessary anymore?
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

/*
var sc_runLaterMethods = [];

function sc_runRunLaterMethods() {
   while (sc_runLaterMethods.length > 0) {
      var toRunLater = sc_runLaterMethods.slice(0);
      sc_runLaterMethods = [];
      for (var i = 0; i < toRunLater.length; i++) {
         var rlm = toRunLater[i];
         try {
            rlm.method.call(rlm.thisObj);
         }
         catch (e) {
            sc_logError("Exception: " + e + " in run later method: " + rlm + " stack:" + e.stack);
         }
      }
   }
   sc_runLaterScheduled = false;
}

// Set this to true when you need to pause runLaters - e.g. wait till you are about to do the next UI refresh.
var sc_runLaterScheduled = false;

*/

function sc_methodCallback(thisObj, method) {
    return function() {
      var _this = thisObj;
      method.call(_this);
   };
}

function sc_addScheduledJob(thisObj, method, timeInMillis, repeat) {
   var f = repeat ? setInterval : setTimeout;
   return f(sc_methodCallback(thisObj, method),timeInMillis);
}

/*
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
*/

function sc_hasProp(obj, prop) {
   if (obj.hasOwnProperty)
      return obj.hasOwnProperty(prop);
   return obj[prop] !== undefined;
}

// e.g. mouseDownEvent -> mousedown
function sc_cvtEventPropToName(eventPropName) {
   return eventPropName.substring(0, eventPropName.indexOf("Event")).toLowerCase();
}

function sc_updatePlist(plist, props) {
   var op = syncMgr[plist];
   if (op) {
      np = [];
      for (var i = 0; i < op.length; i++) {
         var j;
         for (j = 0; j < props.length; j++) {
            if (props[j] === op[i])
               break;
         }
         if (j === props.length)
            np.push(op[i]);
      }
      syncMgr[plist] = op.concat(np);
   }
   else {
      np = props;
      syncMgr[plist] = np;
   }
   return np;
}

function sc_refresh() { // Called at the end of loading a page - in case autoSync is turned on, kick off the first autoSync
   sc_log("sc_refresh() called");
   if (!sc_ClientSyncManager_c.defaultRealTime) {
      sc_log("real time disabled");
      syncMgr.syncDestination.realTime = false;
   }
   else
      sc_log("real time enabled");
   syncMgr.postCompleteSync();
}

function sc_logError(str) {
   if (window.sc_errorCount === undefined)
      window.sc_errorCount = 0;
   window.sc_errorCount++;
   console.error(str);
   sc_rlog(str);
}

function sc_rlog(str) {
   if (sc_PTypeUtil_c && sc_PTypeUtil_c.testMode) {
      var log = window.sc_consoleLog;
      if (log === undefined)
         window.sc_consoleLog = log = [str];
      else
         log.push(str);
   }
}

function sc_log(str) {
   if (sc_PTypeUtil_c && !sc_PTypeUtil_c.testVerifyMode)
      str = sc_logPrefix() + str;
   sc_rlog(str);
   console.log(str);
}

function sc_getConsoleLog() {
   if (window.sc_consoleLog)
      return window.sc_consoleLog.join("\n");
   return "<empty js console>";
}

function sc_logPrefix() {
   if (typeof sc_testVerifyMode !== undefined && sc_testVerifyMode)
      return "";
   return sc_getTimeDelta(sc$startTime, new Date().getTime());
}

function sc_getTimeDelta(startTime, now) {
   if (startTime == 0)
      return "<server not yet started!>";
   var sb = new Array()
   var elapsed = now - startTime;
   sb.push("+");
   var remainder = false;
   if (elapsed > 60*60*1000) {
      var hrs = Math.trunc(elapsed / (60*60*1000));
      elapsed -= hrs * 60*60*1000;
      if (hrs < 10)
         sb.push("0");
      sb.push(hrs);
      sb.push(":");
      remainder = true;
   }
   if (elapsed > 60*1000 || remainder) {
      var mins = Math.trunc(elapsed / (60*1000));
      elapsed -= mins * 60*1000;
      if (mins < 10)
         sb.push("0");
      sb.push(mins);
      sb.push(":");
   }
   if (elapsed > 1000 || remainder) {
      var secs = Math.trunc(elapsed / 1000);
      elapsed -= secs * 1000;
      if (secs < 10)
         sb.push("0");
      sb.push(secs);
      sb.push(".");
   }
   if (elapsed > 1000) // TODO: remove this - diagnostics only
      console.error("bad time in sc_getTimeDelta");
   if (elapsed < 10)
      sb.push("00");
   else if (elapsed < 100)
      sb.push("0");
   sb.push(Math.trunc(elapsed));
   sb.push(":");
   return sb.join("");
}

Error.prototype.printStackTrace = function() {  // Used in generated code
   if (this.stack)
      sc_log(this.stack);
}

sc_PTypeUtil_c = {
   postHttpRequest: function(url, postData, contentType, listener) {
      var httpReq = new XMLHttpRequest();
      httpReq.open("POST", url, true);
      httpReq.onload = function(evt) {
         var stat = httpReq.status;
         if (stat == 200)
            listener.response(httpReq.responseText);
         else {
            // 205 - sync reset
            // 410 - server shutting down - stop polling
            // 0 - server is gone already
            if (stat != 205 && stat != 410 && stat != 0)
               sc_logError("server session lost");
            // This may be the 'reset' request which is not an error
            //sc_logError("Non status='200' response to POST: status=" + httpReq.status + ": " + httpReq.statusText + " response: " + httpReq.responseText);
            // Called below in onreadystatechange
            else
               listener.error(stat, httpReq.statusText);
         }
      }
      httpReq.onreadystatechange = function (evt) {
         if (httpReq.readyState == 4) {
            var stat = httpReq.status;
            if(stat != 200 && stat != 205) {
               if (stat !== 410 && stat !== 0)
                  sc_logError("Return status: " + stat + " for: " + url);
               else
                  sc_log("Return status: " + stat + " for: " + url);
               listener.error(httpReq.status, httpReq.statusText);
            }
         }
      }
      /*
      httpReq.onabort = httpReq.onError = function(evt) {
         sc_logError("aborted response: " + httpReq.status);
         sc_logError("Aborted response to POST: " + httpReq.status + ": " + httpReq.statusText);
         listener.error(httpReq.status, httpReq.statusText);
      }
      */
      if (contentType !== null)
         httpReq.setRequestHeader("Content-type", contentType);

      httpReq.send(postData);
   }
}

// --- DynUtil from scdyn.js

sc_DynUtil_c = {
   getPropertyValue: function(obj, prop) {
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
         if (arguments.length === 2 || !ignoreError)
            throw new Error("Object: " + sc_instId(obj) + " missing property: " + prop);
         else
            res = null;
      }
      return res;
   },
   setPropertyValue: function(obj, prop, val) {
      var setMethod = obj[sc_setName(prop)];
      if (setMethod !== undefined)
         setMethod.call(obj, val);
      else
         obj[prop] = val;
   },
   dispose: function(obj) {} // placeholder - anything to do here?

}

// --- Data binding

sc_Bind_c = {
   trace:false,
   sendChangedEvent: function(obj,propName, val) {
      if (sc_SyncManager_c.trace || sc_Bind_c.trace)
         sc_log("Sync client change: " + sc_instId(obj) + "." + propName + " = " + val);
      syncMgr.addChange(obj, propName, val);
   }
};

// --- HTML stuff

function js_HTMLElement() {
   this.listenerProps = null; // List of tagObject properties which are being listened to on the server - received via serverTag.props
   this.element = null;
}

js_Element_c = js_HTMLElement_c = js_HTMLElement.prototype;

// The list of attributes which change due to user interaction on the client that we sync to the server for 'serverTags'
js_HTMLElement_c.eventAttNames = [];
// The set of attributes when their value goes to null or "" the attribute name itself is removed
js_HTMLElement_c.removeOnEmpty = {};

js_HTMLElement_c.trace = true;

// Specifies the standard DOM events - each event can specify a set of alias properties.  A 'callback' function is lazily added to each domEvent entry the first time we need to listen for that DOM event on an object
js_HTMLElement_c.domEvents = {clickEvent:{}, dblClickEvent:{}, mouseDownEvent:{}, mouseMoveEvent:{}, mouseDownMoveUp:{},
                              mouseOverEvent:{aliases:["hovered"], computed:true}, mouseOutEvent:{aliases:["hovered"], computed:true}, 
                              mouseUpEvent:{}, keyDownEvent:{}, keyPressEvent:{}, keyUpEvent:{}, submitEvent:{}, changeEvent:{}, blurEvent:{}, focusEvent:{}, 
                              resizeEvent:{aliases:["clientWidth","clientHeight","offsetWidth","offsetHeight"]}};

// the reverse direction for the aliases field of the domEvent entry
js_HTMLElement_c.domAliases = {clientWidth:"resizeEvent", clientHeight:"resizeEvent", offsetWidth:"resizeEvent", offsetHeight:"resizeEvent", 
                               hovered:["mouseOverEvent","mouseOutEvent"]};
// Initialize the domEvent properties as null at the class level so we do not have to maintain them for each tag instance.
var domEvents = js_HTMLElement_c.domEvents;
for (var prop in domEvents) {
   if (domEvents.hasOwnProperty(prop))
      js_HTMLElement_c[prop] = null;
}

js_HTMLElement_c.getId = function() {
   if (this.element == null)
      return null;
   return this.element.id;
};

js_HTMLElement_c.eventHandler = function(event, listener) {
   var elem = event.currentTarget;
   if (elem === document && listener.mouseDownElem) {
      elem = listener.mouseDownElem;
   }
   js_HTMLElement_c.processEvent(elem, event, listener);
};

js_HTMLElement_c.processEvent = function(elem, event, listener) {
   var scObj = elem.scObj;
   var ops = listener.otherProps;
   if (scObj !== undefined) {

      // Add this as a separate field so we can use the exposed parts of the DOM api from Java consistently
      event.currentTag = scObj;
      var eventValue, otherEventValues;

      if (listener.alias != null) {
         // e.g. clientWidth or hovered - properties computed from other DOM events
         // hovered depends on mouseOut/In - so we fire the change event here as necessary to reflect the proper state
         var computed = listener.computed;
         var origValue = null;
         if (computed) {
            origValue = scObj[listener.propName];
            // The getX method (e.g. getHovered) needs the info from the event to compute it's value properly
            scObj[listener.scEventName] = event;
         }
         // Access this for logs and so getHovered is called to cache the value of "hovered"
         eventValue = sc_DynUtil_c.getPropertyValue(scObj, listener.propName);
         if (ops) {
            otherEventValues = [];
            for (var opi = 0; opi < ops.length; opi++) {
               otherEventValues.push(sc_DynUtil_c.getPropertyValue(scObj, ops[opi]));
            }
         }

         if (computed) {
            scObj[listener.scEventName] = null;

            // Don't fire events if we did not actually change the value.  For hovered for example, mousein/out events may go to child elements in which case we ignore them.
            if (sc_DynUtil_c.equalObjects(origValue, eventValue))
               return;
         }
      }
      // A regular domEvent like clickEvent - populate the event property in the object
      else {
         // e.g. clickEvent, resizeEvent
         eventValue = event;
         scObj[listener.propName] = event;
      }

      if (listener.scEventName === "changeEvent")
         scObj.preChangeHandler(event);

      if (js_Element_c.trace && listener.scEventName != "mouseMoveEvent")
         sc_log("tag event: " + listener.propName + ": " + listener.scEventName + " = " + eventValue);
      sc_Bind_c.sendChangedEvent(scObj, listener.propName, eventValue);
      if (ops) {
         for (opi = 0; opi < ops.length; opi++) {
            sc_Bind_c.sendChangedEvent(scObj, ops[opi], otherEventValues[opi]);
         }
      }

      if (listener.scEventName === "changeEvent")
         scObj.postChangeHandler(event);

      if (listener.scEventName === "mouseDownMoveUp") {
         if (event.type === "mousedown") {
            listener.mouseDownElem = elem;
            sc_addEventListener(document, "mousemove", listener.callback);
            sc_addEventListener(document, "mouseup", listener.callback);
         }
         else if (event.type === "mouseup") {
            sc_removeEventListener(document, "mousemove", listener.callback);
            sc_removeEventListener(document, "mouseup", listener.callback);
            delete listener.mouseDownElem;
         }
         else if (event.type !== "mousemove")
            console.error("unrecognized event type!");
      }

      // TODO: for event properties should we delete the property here or set it to null?  flush the queue of events if somehow a queue is enabled here?
   }
   else
      sc_log("Unable to find scObject to update in eventHandler");
};

js_HTMLElement_c.preChangeHandler = function(event) {
}

js_HTMLElement_c.postChangeHandler = function(event) {
}

js_HTMLElement_c.getOffsetWidth = function() {
   if (this.element == null)
      return 0;
   return this.element.offsetWidth;
};

js_HTMLElement_c.getOffsetHeight = function() {
   if (this.element == null)
      return 0;
   return this.element.offsetHeight;
};

js_HTMLElement_c.getClientWidth = function() {
   if (this.element == null)
      return 0;
   return this.element.clientWidth;
};

js_HTMLElement_c.getClientHeight = function() {
   if (this.element == null)
      return 0;
   return this.element.clientHeight;
};

js_HTMLElement_c.getHovered = function() {
   var mouseEvent = this.mouseOverEvent;
   var isMouseOver;
   if (mouseEvent == null) {
      mouseEvent = this.mouseOutEvent;
      if (mouseEvent != null)
         isMouseOver = false;
   }
   else
      isMouseOver = true;
   if (mouseEvent != null) {
      var related = mouseEvent.relatedTarget;
      // Suppress over and out events which are going to/from children of the same node
      if (this.element != null && (related == null || !this.element.contains(related))) {
         sc_log("Setting hovered: " + isMouseOver + " on: " + this.getId());
         this.hovered = isMouseOver;
      }
      else
         sc_log("Skipping hovered for: " + isMouseOver + " on: " + this.getId());
   }
   if (this.hovered !== undefined)
      return this.hovered;
   return false;
};

// Update this tagObject from the DOM element.  Any properties we derive from the DOM element
// must be updated in the tag object, so that we can detect when they change in the future.
js_HTMLElement_c.updateFromDOMElement = function(newElement) {
   if (newElement !== this.element) {
      var orig = this.element;
      if (orig !== null) {
         delete orig.scObj;
      }

      this.element = newElement;
      if (orig === null && newElement !== null) {
         // This adds listeners to the tag object to update the DOM, which we do not need for serverTag mode.
         // here we only need to add the listeners for the DOM events which change on the client to send to the server.
         //this.addAttributeListener();
      }

      if (newElement !== null) {
         //sc_id(newElement); add for debugging to make it easier to identify unique elements

         // This can happen if
         if (newElement.scObj !== undefined) {
            sc_log("Warning: replacing object: " + sc_instId(newElement.scObj) + " with: " + sc_instId(this) + " for tag: " + this.tagName);
         }
         newElement.scObj = this;
      }
      this.domChanged(orig, newElement);
      //this.updateChildDOMs(); - No need to update the childDOMs since we do not have the tagObject tree on the client
   }
};

js_HTMLElement_c.removeDOMEventListeners = function(origElem) {
   if (origElem !== null) {
      var curListeners = this._eventListeners;
      if (curListeners != null) {
         for (var i = 0; i < curListeners.length; i++) {
            var listener = curListeners[i];
            sc_removeEventListener(origElem, listener.eventName, listener.callback);
         }
         this._eventListeners = null;
      }
   }
}

// Called when the DOM element associated with the tag object has changed.
js_HTMLElement_c.domChanged = function(origElem, newElem) {
   this.removeDOMEventListeners(origElem);
   if (newElem !== null) {
      var curListeners = this.getDOMEventListeners();
      if (curListeners != null) {
         if (this._eventListeners != null)
            sc_log("*** error: replacing element event listeners");
         for (var i = 0; i < curListeners.length; i++) {
            var listener = curListeners[i];

            if (!listener.alias) {
               if (this[listener.propName] === undefined)
                  this[listener.propName] = null; // set the event property to null initially the first time we have someone listening on it.  this is too late but do we want to initialize all of these fields to null on every tag object just so they are null, not undefined?   Just do not override an existing value or refreshBinding fires when we do not want it to
            }
            else {// Now that we know the value of the aliased property (e.g. innerHeight) we need to send a change event cause it changes once we have an element.
               sc_Bind_c.sendChangedEvent(this, listener.propName, sc_DynUtil_c.getPropertyValue(this, listener.propName));
               var ops = listener.otherProps;
               if (ops) {
                  for (var opi = 0; opi < ops.length; opi++) {
                     sc_Bind_c.sendChangedEvent(this, ops[opi], sc_DynUtil_c.getPropertyValue(this, ops[opi]));
                  }
               }
            }

            // Only IE supports the resize event on objects other than the window.
            if (listener.eventName == "resize" && !newElem.attachEvent) {
               sc_addEventListener(window, listener.eventName,
                       function(evt) {
                          js_HTMLElement_c.processEvent.call(window, newElem, evt, listener);
                       }
               );
            }
            sc_addEventListener(newElem, listener.eventName, listener.callback);
         }
         this._eventListeners = curListeners;
      }
   }
}

js_HTMLElement_c.initDOMListener = function(listener, prop, eventPropName) {
   if (eventPropName == null)
      eventPropName = prop;
   else
      listener.alias = true; // This is like clientWidth which is mapped to a separate resizeEvent
   // Convert from the sc event name, e.g. clickEvent to click
   listener.eventName = eventPropName === "mouseDownMoveUp" ? "mousedown" : sc_cvtEventPropToName(eventPropName);
   listener.scEventName = eventPropName;
   listener.propName = prop;
   listener.callback = sc_newEventArgListener(js_HTMLElement_c.eventHandler, listener);
   // For resizeEvent we have four properties clientWidth, clientHeight, outerWidth, outerHeight all set from the same event and listener
   var als = listener.aliases;
   if (als && als.length > 1) {
      var ops = [];
      for (var i = 0; i < als.length; i++) {
         if (als[i] !== prop)
            ops.push(als[i]);
      }
      listener.otherProps = ops;
   }
}

function js_initDOMListener(listener, prop, eventName, res) {
   // For efficiency, each domEvent stores initially an empty object.  We lazily create the string names and callback to avoid creating them for each instance.
   if (listener.callback == null) {
      js_HTMLElement_c.initDOMListener(listener, prop, eventName);
   }
   if (res == null)
      res = [];
   res.push(listener);
   return res;
}

// For the given server tag instance, which has a set of 'listenerProps' - i.e. those domEvents which are used in bindings on the
// server - return the array of dom event listeners.  The domEvents like clickEvent, are converted to properties
// of the server tag object and sent over on the next sync.
js_HTMLElement_c.getDOMEventListeners = function() {
   var listenerProps = this.listenerProps;
   var res = null;
   var domEvents = this.domEvents;
   var domAliases = this.domAliases;

   for (var i = 0; i < listenerProps.length; i++) {
      prop = listenerProps[i];
      var listener = null;
      var eventName = null;
      var handled = false;
      if (domEvents.hasOwnProperty(prop)) {
         listener = domEvents[prop];
      }
      else if (domAliases.hasOwnProperty(prop)) {
         var eventNameList = domAliases[prop];
         // The alias may require multiple events - e.g. mouseOverEvent and mouseOutEvent for hovered
         if (eventNameList.constructor === Array) {
            for (var eix = 0; eix < eventNameList.length; eix++) {
               var nextEventName = eventNameList[eix];
               listener = domEvents[nextEventName];
               res = js_initDOMListener(listener, prop, nextEventName, res);
            }
            handled = true;
         }
         else {
            eventName = eventNameList;
            listener = domEvents[eventName];
         }
      }
      if (listener != null && !handled) {
         res = js_initDOMListener(listener, prop, eventName, res);
      }
   }
   return res;
}

js_HTMLElement_c.click = function() {
   var evt = new MouseEvent("click");
   evt.currentTarget = evt.target = this; // TODO: do not appear to be settable?
   this.clickEvent = evt;
   evt.currentTag = this;
   sc_Bind_c.sendChangedEvent(this, "clickEvent", evt);
}

js_HTMLElement_c.destroy = function() {
   var origElem = this.element;
   this.element = null;
   this.domChanged(origElem, null);
}

function js_Input() {
   js_HTMLElement.call(this);
   this.liveEdit = "on";
   this.liveEditDelay = 0;
   this.lastSequence = 0;
}
js_Input_c = sc_newClass("Input", js_Input, js_HTMLElement);
js_Input_c.eventAttNames = js_HTMLElement_c.eventAttNames.concat(["value", "checked", "changeEvent", "clickCount"]);
js_Input_c.removeOnEmpty = {value:true};

js_Input_c.domChanged = function(origElem, newElem) {
   if (this.type == "button") {
      js_Button_c.domChanged.call(this, origElem, newElem);
      return;
   }
   if (origElem !== null) {
      sc_removeEventListener(origElem, 'change', js_Input_c.doChangeEvent);
      sc_removeEventListener(origElem, 'keyup', js_Input_c.doChangeEvent);
   }
   if (newElem !== null) {
      sc_addEventListener(newElem, 'change', js_Input_c.doChangeEvent);
      sc_addEventListener(newElem, 'keyup', js_Input_c.doChangeEvent);
   }
   // We want the doChangeEvent to fire here before the change event fires on the DOM so that value is set
   // before the changeEvent property.
   js_HTMLElement_c.domChanged.call(this, origElem, newElem);
}

js_Input_c.updateFromDOMElement = function(newElem) {
   js_HTMLElement_c.updateFromDOMElement.call(this, newElem);
   this.value = newElem.value;
   this.checked = newElem.checked;
}

js_Input_c.preChangeHandler = function(event) {
   // This is the sequence number of this input tag's value. If it changes before we get back the response it might
   // cause us to ignore a change to 'value' in the response because it's stale.
   this.lastSequence = syncMgr.syncSequence;
   if ((this.liveEdit == "off" || this.liveEditDelay != 0 || (this.liveEdit == "change" && event.type == "keyup"))) {
       sc_ClientSyncManager_c.syncDelaySet = true;
       sc_ClientSyncManager_c.currentSyncDelay = this.liveEditDelay != 0 ? this.liveEditDelay : -1;
       cs = true;
   }
}

js_Input_c.postChangeHandler = function(event) {
   sc_ClientSyncManager_c.syncDelaySet = false;
}

js_Input_c.doChangeEvent = function(event) {
   var elem = event.currentTarget ? event.currentTarget : event.srcElement;
   var scObj = elem.scObj;
   if (scObj !== undefined) {
      scObj.preChangeHandler(event);
      if (scObj.setValue) {
         scObj.setValue(this.value);
         if (this.value === "" && scObj.removeOnEmpty.value != null)
            this.removeAttribute("value");
      }
      if (scObj.setChecked)
         scObj.setChecked(this.checked);

      scObj.postChangeHandler(event);
   }
   else
      sc_log("Unable to find scObject to update in doChangeEvent");
}

js_Input_c.setValue = function(newVal) {
   if (newVal == null)
      newVal = "";
   sc_log("Input.setValue(" + newVal + ")");
   if (newVal != this.value) {
      this.value = newVal;
      if (this.element !== null && this.element.value != newVal)
         this.element.value = newVal;
      sc_log("Input.sendChangedEvent(" + newVal + ")");
      sc_Bind_c.sendChangedEvent(this, "value" , newVal);
   }
   else
      sc_log("Input.setValue - not changed");
}

js_Input_c.getValue = function() {
   return this.value;
}

js_Input_c.setDisabled = function(newVal) {
   if (newVal != this.value) {
      this.disabled = newVal;
      if (this.element !== null && this.element.disabled != newVal)
         this.element.disabled = newVal;
      sc_Bind_c.sendChangedEvent(this, "disabled" , newVal);
   }
}

js_Input_c.getDisabled = function() {
   return this.disabled;
}

js_Input_c.setClickCount = function(ct) {
   if (ct != this.clickCount) {
      this.clickCount = ct;
      sc_Bind_c.sendChangedEvent(this, "clickCount", this.clickCount);
   }
}

js_Input_c.getClickCount = function() {
   return this.clickCount;
}

js_Input_c.setChecked = function(ch) {
   if (ch != this.checked) {
      this.checked = ch;
      if (this.element !== null && this.element.checked != ch)
         this.element.checked = ch;
      sc_Bind_c.sendChangedEvent(this, "checked", ch);
   }
}

js_Input_c.getChecked = function() {
   return this.checked;
}

function js_Select() {
   js_HTMLElement.call(this);
   this.optionDataSource = null;
   this.selectedIndex = -1;
   this.selectedValue = null;
}
js_Select_c = sc_newClass("Select", js_Select, js_HTMLElement);
js_Select_c.eventAttNames = js_HTMLElement_c.eventAttNames.concat([ "selectedIndex", "changeEvent"]);

js_Select_c.doChangeEvent = function(event) {
   var elem = event.currentTarget ? event.currentTarget : js_findCurrentTargetSimple(event.srcElement, "selectedIndex");
   var scObj = elem.scObj;
   if (scObj !== undefined) {
      var ix = elem.selectedIndex;
      scObj.setSelectedIndex(ix);
   }
   else
      sc_log("Unable to find scObject to update in doChangeEvent");
}

js_Select_c.domChanged = function(origElem, newElem) {
   if (origElem != null)
      sc_removeEventListener(origElem, 'change', js_Select_c.doChangeEvent);
   if (newElem != null) {
      sc_addEventListener(newElem, 'change', js_Select_c.doChangeEvent);
   }
   // Must be after the 'change' has updated the selectedIndex/Value properties so the event handler sees the new value
   js_HTMLElement_c.domChanged.call(this, origElem, newElem);
}

js_Select_c.setSelectedIndex = function(newIx) {
   if (newIx != this.selectedIndex) {
      this.selectedIndex = newIx;
      if (this.element != null && this.element.selectedIndex != newIx)
         this.element.selectedIndex = newIx;
      var ds = this.optionDataSource;
      var len = ds == null ? 0 : sc_length(ds);
      if (newIx >= 0 && newIx < len)
         this.setSelectedValue(sc_arrayValue(ds, newIx));
      else
         this.setSelectedValue(null);
      sc_Bind_c.sendChangedEvent(this, "selectedIndex", newIx);
   }
}

js_Select_c.getSelectedIndex = function() {
   return this.selectedIndex;
}

js_Select_c.setSelectedValue = function(newVal) {
   if (newVal != this.selectedValue) {
      this.selectedValue = newVal;
      var ds = this.optionDataSource;
      var ix = ds == null ? -1 : ds.indexOf(newVal);
      if (ix != this.selectedIndex)
         this.setSelectedIndex(ix);
      sc_Bind_c.sendChangedEvent(this, "selectedValue", newVal);
   }
}

js_Select_c.getSelectedIndex = function() {
   return this.selectedIndex;
}

js_Select_c.setOptionDataSource = function(newDS) {
   if (newDS !== this.optionDataSource) {
      if (this.selectListener == null) {
         this.selectListener = new js_SelectListener(this);
         sc_Bind_c.addListener(this, "optionDataSource", this.selectListener, sc_IListener_c.VALUE_VALIDATED);
      }
      this.optionDataSource = newDS;
   }
   sc_Bind_c.sendChangedEvent(this, "optionDataSource" , newDS);
}

js_Select_c.getOptionDataSource = function() {
   return this.optionDataSource;
}

js_Select_c.preChangeHandler = js_Input_c.preChangeHandler;
js_Select_c.postChangeHandler = js_Input_c.postChangeHandler;

function js_Form() {
   js_HTMLElement.call(this);
   this.submitCount = 0;
   this.submitInProgress = false;
   this.submitError = null;
}
js_Form_c = sc_newClass("Form", js_Form, js_HTMLElement);
js_Form_c.eventAttNames = js_HTMLElement_c.eventAttNames.concat([ "submitCount", "submitEvent"]);
js_Form_c.submitEvent = function(event) {
   var elem = event.currentTarget ? event.currentTarget : js_findCurrentTargetSimple(event.srcElement, "submitCount");
   var scObj = elem.scObj;
   if (scObj !== undefined)
      scObj.setSubmitCount(scObj.getSubmitCount()+1);
   else
      sc_log("Unable to find scObject to update in submitEvent");
}

js_Form_c.submit = function() {
   if (this.element)
      this.element.submit();
   else
      sc_log("No dom element for submit call");
}

js_Form_c.sendSubmitEvent = function() {
   var evt = new Event('submit');
   evt["currentTarget"] = evt["target"] = this; // not using "." here because intelliJ complains these are constant - will any browsers barf on this?  If so we'll need to just create a new object and copy over any fields we need.
   this.submitEvent = evt;
   sc_Bind_c.sendChangedEvent(this, "submitEvent", evt);
}

js_Form_c.submitFormData = function(url) {
   if (this.element) {
      var formData = new FormData(this.element);
      /*
      var enctype = this.element.enctype;
      if (!enctype)
         enctype = "application/x-www-form-urlencoded";
      */
      var listener = {
         obj:this,
         response: function() {
            this.obj.setSubmitError(null);
            this.obj.setSubmitInProgress(false);
         },
         error: function(code, msg) {
            this.obj.setSubmitError(code);
            this.obj.setSubmitInProgress(false);
            sc_error("Error from submitFormData result: " + code + ": " + msg);
         }
      };
      this.setSubmitInProgress(true);
      this.setSubmitCount(this.getSubmitCount()+1);
      sc_PTypeUtil_c.postHttpRequest(url, formData, null, listener);
   }
}

js_Form_c.domChanged = function(origElem, newElem) {
   js_HTMLElement_c.domChanged.call(this, origElem, newElem);
   if (origElem != null)
      sc_removeEventListener(origElem, 'submit', js_Form_c.submitEvent);
   if (newElem != null)
      sc_addEventListener(newElem, 'submit', js_Form_c.submitEvent);
}

js_Form_c.setSubmitCount = function(ct) {
   if (ct != this.submitCount) {
      this.submitCount = ct;
      sc_Bind_c.sendChangedEvent(this, "submitCount" , this.submitCount);
   }
}

js_Form_c.getSubmitCount = function() {
   return this.submitCount;
}

js_Form_c.setSubmitInProgress = function(v) {
   this.submitInProgress = v;
   sc_Bind_c.sendChangedEvent(this, "submitInProgress" , this.submitInProgress);
}

js_Form_c.getSubmitInProgress = function() {
   return this.submitInProgress;
}

js_Form_c.setSubmitError = function(e) {
   this.submitError = e;
   sc_Bind_c.sendChangedEvent(this, "submitError" , this.submitError);
}

js_Form_c.getSubmitError = function() {
   return this.submitError;
}

function js_Option() {
   js_HTMLElement.call(this);
   this.optionData = null;
   this.selected = false;
}
js_Option_c = sc_newClass("Option", js_Option, js_HTMLElement);
js_Option_c.eventAttNames = js_HTMLElement_c.eventAttNames.concat(["selected", "optionData"]);

js_Option_c.setOptionData = function(dv) {
   this.optionData = dv;
   sc_Bind_c.sendChangedEvent(this, "optionData", dv);
}
js_Option_c.getOptionData = function() {
   return this.optionData;
}

js_Option_c.setSelected = function(newSel) {
   if (newSel != this.selected) {
      this.selected = newSel;
      if (this.element !== null && this.element.selected != newSel)
         this.element.selected = newSel;
      sc_Bind_c.sendChangedEvent(this, "selected" , newSel);
   }
}

js_Option_c.getSelected = function() {
   return this.selected;
}

function js_RepeatTag() {}

js_RepeatTag_c = sc_newClass("RepeatWrapper", js_RepeatTag, js_HTMLElement);

// For repeat server tags, no need to add any listeners
js_RepeatTag_c.updateFromDOMElement = function(newElement) {
   if (newElement !== this.element) {
      var orig = this.element;
      if (orig !== null) {
         delete orig.scObj;
      }

      this.element = newElement;
      if (newElement !== null) {
         //sc_id(newElement); enable for debugging to make it easier to identify unique elements

         // This can happen if
         if (newElement.scObj !== undefined) {
            sc_log("Warning: replacing object: " + sc_instId(newElement.scObj) + " with: " + sc_instId(this) + " for tag: " + this.tagName);
         }
         newElement.scObj = this;
      }
   }
}

js_RepeatTag_c.isRepeatId = function(id) {
   var ix = id.indexOf('_Repeat');
   if (ix === -1)
      return false;
   var eix = ix + '_Repeat'.length;
   if (eix === id.length)
      return true;
   return id.charAt(eix) === '_';
}

js_RepeatTag_c.isRepeatEndId = function(id) {
   var ix = id.indexOf('_Repeat')
   var ex = id.indexOf('_end');
   if (ix === -1 || ex === -1) 
      return false;
   return true;
}

function js_Document() {
   this.element = document;
   document.scObj = this;
}
js_Document_c = sc_newClass("Document", js_Document, js_HTMLElement);

js_Document_c.getId = function() {
   return "document";
};

js_HTMLElement_c.tagNameToType = {input:js_Input, form:js_Form, select:js_Select, option:js_Option};
js_ServerTag_c = {
   equals: function(s1,s2) {
      return s1 === s2 || (s1 != null && s2 != null && s1.id === s2.id); // TODO: check if props have changed
   }
};

function sc_SyncListener(anyChanges) {
   this.anyChanges = anyChanges;
   this.syncSequence = syncMgr.syncSequence;
}

sc_SyncListener_c = sc_newClass("SyncListener", sc_SyncListener, null);

sc_SyncListener_c.syncResponse = function() {
   var res = syncMgr.pendingSends.pop();
   if (syncMgr.pendingSync) {
      syncMgr.pendingSync = false;
      syncMgr.scheduleSync(0);
   }
   if (this.anyChanges)
      syncMgr.numSendsInProgress--;
   else
      syncMgr.numWaitsInProgress--;

   if (syncMgr.numSendsInProgress < 0 || syncMgr.numWaitsInProgress < 0)
      sc_logError("Invalid case in syncResponse");
   return res;
}


sc_SyncListener_c.response = function(responseText) {
   sc_log("in response handler");
   this.syncResponse();
   var syncLayerStart = "sync:";
   syncMgr.connected = true; // Success from server means we are connected
   var nextText = responseText;
   // A string of the form sync:json:len:data:sync:js:len:data
   // Supporting more than one format - e.g. json and js when there are code updates to be applied
   while (nextText.length > 0) {
      var processDef = null;
      if (nextText.startsWith(syncLayerStart)) {
         var endLangIx = nextText.indexOf(':', syncLayerStart.length);
         if (endLangIx !== -1) {
            var lang = nextText.substring(syncLayerStart.length, endLangIx);
            var lenStart = endLangIx + 1;
            if (nextText.length > lenStart) {
               var endLenIx = nextText.indexOf(':', lenStart);
               if (endLenIx !== -1) {
                  var lenStr = nextText.substring(lenStart, endLenIx);
                  var syncLen = parseInt(lenStr);
                  if (isNaN(syncLen)) {
                     sc_logError("Invalid length in sync response: " + e);
                     break;
                  }
                  var layerDefStart = endLenIx + 1;
                  processDef = nextText.substring(layerDefStart, layerDefStart + syncLen);
                  nextText = nextText.substring(layerDefStart + syncLen + 1);
                  if (lang === "json")
                     syncMgr.applySyncLayer("json", processDef, this.syncSequence, this.anyChanges ? "send" : "wait");
                  else if (lang === "js")
                     eval(processDef);
                  else
                     sc_logError("Unrecognized language in sync response: " + lang);
               }
            }
         }
      }
      else {
         sc_logError("Unrecognized text in sync listener response");
         break;
      }
   }
   syncMgr.postCompleteSync();
};

sc_SyncListener_c.error = function(code, text) {
   sc_log("in error handler");
   var errReq = this.syncResponse();
   if (code === 205) { // session on server has expired - send the reset state and the original error request to be reapplied
      sc_logError("*** Server session lost - sending empty reset");
      var resetJ = {sync:[sc$resetState]};
      syncMgr.writeToDestination(JSON.stringify(resetJ, null, 3) + " " + errReq,"&reset=true");
   }
   else if (code === 410) { // server shutdown
      syncMgr.connected = false;
      syncMgr.syncDestination.realTime = false;
      sc_logError("*** Server shutdown - aborting request: " + errReq);
   }
   else if (code === 500 || code === 0) {
      syncMgr.connected = false;
      sc_logError("*** Server reported error code: " + code + " aborting sync: " + errReq);
   }
   else
      sc_logError("*** Unrecognized server error: " + code + " aborting sync: " + errReq);
   syncMgr.postCompleteSync();
};

syncMgr = sc_SyncManager_c = {
   trace: false,
   serverTags:{},
   serverTagList:[],
   tagObjects:{},
   pendingChanges:[],
   changesByObjId:{},
   syncScheduled:false,
   pendingSync:false,
   numSendsInProgress:0,
   numWaitsInProgress:0,
   maxSends:1,
   eventIndex:0,
   syncSequence:0,
   syncDestination:{realTime:true},
   waitTime:1200000, // TODO: make this configurable in the page or URL?
   pollTime: 500,
   pendingSends:[],
   autoSyncScheduled: false,
   connected: true,
   refreshTagsScheduled: false,
   windowSyncProps: null,
   documentSyncProps: null,
   applySyncLayer: function(lang,json,syncSequence,detail) {
      if (sc_SyncManager_c.trace) {
         sc_log("Sync applying server changes (from: " + (detail ? detail : "init") + " request): " + json);
     }
      if (lang !== "json") {
         sc_logError("unable to apply non-json sync layer");
         return;
      }
      var so = JSON.parse(json);
      var sl = so.sync;
      if (!sl) {
         sc_logError("Invalid JSON for applySyncLayer - missing top-level 'sync'");
         return;
      }
      var curPkg = "";
      var newServerTags = {}; // created in this apply call
      var updatedServerTags = {}; // when the list of server tags has changed, contains the new serverTags
      var newServerTagList = [];
      var removedServerTagIds = {};
      var serverTagsChanged = false; // has the list of serverTags changed
      var serverTagsReset = false; // Is this a brand new set of server tags or updates to the existing list
      var needsTagRefresh = false; // has the body of any element changed which might contain serverTags requiring a refresh
      for (var i = 0; i < sl.length; i++) {
         var cmd = sl[i];
         var newArgs = cmd["$new"];
         if (newArgs) {
            var cl = newArgs[1];
            if (cl === "sc.js.ServerTag")
               newServerTags[newArgs[0]] = {name:newArgs[0]};
            // These map to system objects so no server tag instance
            else if (cl !== "sc.js.ServerTagManager" && cl !== "sc.lang.html.Location" && cl !== "sc.lang.html.Document")
               sc_logError("Unrecognized class for $new in stags.js: " + cl);
            continue;
         }
         var newPkg = cmd["$pkg"];
         if (newPkg != null) {
            curPkg = newPkg;
            continue;
         }
         var keys = Object.keys(cmd);
         if (keys.length === 1) {
            var name = keys[0];
            if (curPkg === "sc.js") {
               if (name.startsWith("st_")) {
                  var st = newServerTags[name];
                  if (!st)
                     st = syncMgr.serverTags[name];
                  var stProps = cmd[name];
                  if (st) {
                     if (stProps.id)
                        st.id = stProps.id;
                     if (stProps.props)
                        st.props = stProps.props;
                     if (stProps.liveEdit === undefined)
                        st.liveEdit = "on";
                     else
                        st.liveEdit = stProps.liveEdit;
                     if (stProps.liveEditDelay === undefined)
                        st.liveEditDelay = 0;
                     else
                        st.liveEditDelay = stProps.liveEditDelay;
                  }
                  else
                     sc_logError("No ServerTag for modify");
               }
               else if (name === "PageServerTagManager") {
                  // Some changes being made to the server tag manager.  Now, when any changes are made, we send the entire new list
                  // so that means we can clean up old tag objects associated with server tags we no longer need to listen (or that may no longer exist)
                  serverTagsChanged = true;
                  var pstm = cmd[name];
                  var stObj = pstm.serverTags;
                  if (stObj)
                     serverTagsReset = true;
                  else
                     stObj = pstm.newServerTags;
                  if (stObj) {
                     // TODO: loop over keys of the stObj - lookup st in either existing or new serverTags list.  Set newServerTagList.
                     var elemIds = Object.keys(stObj);
                     for (var elIx = 0; elIx < elemIds.length; elIx++) {
                        var elemId = elemIds[elIx];
                        var stId = stObj[elemId];
                        if (stId.startsWith("ref:")) {
                           stId = stId.substring("ref:".length);
                           var refServerTag = newServerTags[stId];
                           if (!refServerTag)
                              refServerTag = syncMgr.serverTags[stId];
                           if (!refServerTag)
                              sc_logError("Server tag reference not found");
                           else {
                              newServerTagList.push(refServerTag);
                              updatedServerTags[stId] = refServerTag;
                           }
                        }
                        else
                           sc_logError("Invalid serverTag ref");
                     }
                  }
                  if (!serverTagsReset) {
                     var remIds = pstm.removedServerTags;
                     if (remIds) {
                        var remArr = Object.keys(remIds);
                        for (var rmIx = 0; rmIx < remArr.length; rmIx++) {
                           var remId = remIds[elIx];
                           removedServerTagIds[remId] = true;
                        }
                     }
                  }
               }
            }
            // Dynamic style sheet updated on the server - uses innerHTML for the new CSS content
            else if (curPkg === "_css") {
               var newStyleBody = cmd[name].innerHTML;
               var cssId = "_css." + name;
               var style = document.getElementById(cssId);
               if (style == null) {
                  var heads = document.getElementsByTagName("head");
                  if (heads == null) {
                     console.error("No head tag for dynamic css");
                  }
                  else {
                     style = document.createElement("style");
                     style.id = cssId; // So we can find this style again if the parent has to refresh
                     style.type = "text/css";
                     style.innerHTML = newStyleBody;
                     heads[0].appendChild(style);
                  }
               }
               else {
                  style.innerHTML = newStyleBody;
               }

            }
            else if (name === "Location") {
               var lps = cmd[name];
               var pnames = Object.keys(lps);
               for (var i = 0; i < pnames.length; i++) {
                  var pname = pnames[i];
                  window.location[pname] = lps[pname];
               }
            }
            else if (name === "DynUtil") { // Support some select remote methods for testing
               var mObj = cmd[name];
               var mName = mObj.$meth;
               var callId = mObj.callId;
               if (mName === "evalScript") {
                  var res = eval(mObj.args[0]);
                  syncMgr.addMethReturn(res, callId);
               }
            }
            else {
               var chElem = document.getElementById(name);
               if (chElem === null) {
                  if (name === "body")
                     chElem = document.body;
                  else if (name === "head")
                     chElem = document.head;
               }
               if (chElem) {
                  var chObj = cmd[name];
                  var props = Object.keys(chObj);
                  for (var pix = 0; pix < props.length; pix++) {
                     var prop = props[pix];
                     var val = chObj[prop];
                     if (prop === "startTagTxt") {
                        // Avoid two race conditions for typing into an input tag that is being updated
                        if (chElem.tagName == "INPUT" && chElem.scObj) {
                           // User has typed a key that's not yet been received by doChangeEvent
                           if (chElem.value != chElem.scObj.value)
                              sc_log("Out of sync change to: " + name + " startTagTxt because element has changed since this sync was sent");
                           // doChangeEvent has been called since we sent this sync - TODO: we should really only be skipping the 'value' attribute
                           // update in setStartTagTxt - it might be that a 'class' or other update should happen even on a stale edit (but that seems kind of
                           // unlikely too)
                           else if (chElem.scObj.lastSequence > syncSequence)
                              sc_log("Ignoring change to: " + name + " startTagTxt because element has changed since this sync was sent");
                           else
                              syncMgr.setStartTagTxt(chElem, val);
                        }
                        else
                           syncMgr.setStartTagTxt(chElem, val);
                     }
                     else if (prop === "innerHTML") {
                        if (js_RepeatTag_c.isRepeatId(name)) { // repeat tag needs special processing - remove all 'repeat children', then insert the innerHTML after the marker element
                           do {
                              var next = chElem.nextElementSibling;
                              if (next == null) {
                                 console.error("Failed to find _Repeat_end tag for replacing repeat body");
                                 break;
                              }
                              if (js_RepeatTag_c.isRepeatEndId(next.id))
                                 break;
                              chElem.parentNode.removeChild(next);
                           } while (true);
                           chElem.insertAdjacentHTML("afterend", val);
                        }
                        else {
                           chElem.innerHTML = val;
                        }
                        needsTagRefresh = true;
                     }
                     else if (prop === "value") {
                        if (chElem.tagName == "INPUT" && chElem.scObj && chElem.value != chElem.scObj.value)
                           sc_log("Out of sync change to: " + name + " input.value because element has changed since this sync was sent");
                        else if (chElem.scObj && chElem.scObj.lastSequence > syncSequence)
                           sc_log("Ignoring change to " + name + ".value that's been changed since this sync started");
                        else {
                           chElem.value = val;
                           if (chElem.scObj)
                              chElem.scObj.value = val;
                        }
                     }
                     else if (prop === "checked")
                        chElem.checked = val;
                     else if (prop === "selectedIndex")
                        chElem.selectedIndex = val;
                     else if (prop === "style")
                        chElem.style = val;
                     else if (prop === "$meth") {
                        var args = chObj["args"];
                        var cid = chObj["callId"];
                        var scElem = chElem.scObj;
                        if (scElem) {
                           var f = scElem[val];
                           if (f) {
                              var mres = f.apply(scElem, args);
                              if (!mres)
                                 mres = null; // cvt undefined to null for 'void' functions
                              syncMgr.addMethReturn(mres, cid);
                           }
                           else {
                              console.error("No method named: " + mn + " for remote call");
                           }
                        }
                        pix = props.length; // finished this command
                     }
                     else
                        sc_logError("Unrecognized property in stags sync layer: " + prop);
                  }
               }
               else {
                  if (js_Element_c.verbose)
                     sc_log("Recording reset state: " + prop + ": " + val);
                  sc$resetState[prop] = val;
               }
            }
         }
         else {
            sc_logError("Unrecognized sync cmd for server tag: " + cmd);
         }
      }
      if (serverTagsChanged) {
         var serverTags = syncMgr.serverTags;
         var tagObjects = syncMgr.tagObjects;
         //var serverTagList = syncMgr.serverTagList;
         for (var j = 0; j < newServerTagList.length; j++) {
            newSt = newServerTagList[j];
            var id = newSt.id;
            if (!id) {
               console.log("no id for server tag!");
               continue;
            }
            var oldSt = serverTags[newSt.name];
            var curTagObj = tagObjects[id];

            if (oldSt != null && js_ServerTag_c.equals(oldSt, newSt) && !needsTagRefresh)
               continue; // No changes to this server tag - unless we have refreshed child tags and so may need to refresh the element reference

            syncMgr.updateServerTag(curTagObj, id, newSt);
         }
         var oldServerTagList = syncMgr.serverTagList;
         for (var k = 0; k < oldServerTagList.length; k++) {
            var oldSt = oldServerTagList[k];
            var id = oldSt.id;
            if (removedServerTagIds[id] || (serverTagsReset && !updatedServerTags[oldSt.name])) {
               var oldTagObj = tagObjects[id];
               if (oldTagObj != null) {
                  delete tagObjects[id];
                  oldTagObj.destroy();
               }
            }
         }
         if (serverTagsReset) {
            syncMgr.serverTagList = newServerTagList;
            syncMgr.serverTags = updatedServerTags;
         }
         else {
            syncMgr.serverTagList = syncMgr.serverTagList.concat(newServerTagList);
            Object.assign(syncMgr.serverTags, updatedServerTags);
         }
      }
      if (needsTagRefresh)
         syncMgr.schedRefreshTags();
   },
   // Called when the DOM body has changed - we might need to update the 'element' attached to one or more tagObject
   // definitions attached to the serverTags list.
   refreshTags: function() {
      syncMgr.refreshTagsScheduled = false;
      var serverTagList = syncMgr.serverTagList;
      var tagObjects = syncMgr.tagObjects;
      for (var j = 0; j < serverTagList.length; j++) {
         var st = serverTagList[j];
         var id = st.id;
         if (!id) {
            console.log("Null id for server tag in refresh!");
            continue;
         }
         var curTagObj = tagObjects[id];
         syncMgr.updateServerTag(curTagObj, id, st);
      }
   },
   // Called to create, or update a server tag object, pointing to the DOM element specified by 'id'.
   // It's called from a server response handler with an optional serverTag info object describing the properties the
   // server is interested in. It returns the resulting tag object
   updateServerTag: function(tagObj, id, serverTag) {
      var element = document.getElementById(id);
      var isRepeat = js_RepeatTag_c.isRepeatId(id);

      if (element != null) {
         if (tagObj == null && element.scObj != null)
            tagObj = element.scObj;
         if (tagObj == null) {
            var tagClass = isRepeat ? js_RepeatTag : js_HTMLElement_c.tagNameToType[element.tagName.toLowerCase()];
            if (tagClass == null)
               tagClass = js_HTMLElement;
            if (tagClass != null) {
               tagObj = new tagClass();
               tagObj.id = id;
               if (serverTag.liveEdit == "off" || serverTag.liveEdit == "change")
                  tagObj.liveEdit = serverTag.liveEdit;
               if (serverTag.liveEditDelay != 0)
                  tagObj.liveEditDelay = serverTag.liveEditDelay;
               var props = serverTag == null || serverTag.props == null ? tagObj.eventAttNames : serverTag.props.concat(tagObj.eventAttNames);

               tagObj.listenerProps = props;
               tagObj.updateFromDOMElement(element);

               syncMgr.tagObjects[id] = tagObj;

               // TODO: addSyncInst logic here - listen for changes on the DOM attributes according to these properties and register a callback that
               // will queue up changes which we convert to the JSON to send to the server
            }
         }
         // DOM element has changed
         else if (tagObj.element !== element) {
            if (js_Element_c.verbose)
               sc_log("Updating DOM element for tagObject with " + id);
            if (serverTag != null && serverTag.props != null) {
               var oldProps = tagObj.listenerProps;
               if (!oldProps || oldProps.length !== serverTag.props.length) {
                  if (js_Element_c.verbose)
                     sc_log("Updating DOM event listeners properties for: " + id);
                  tagObj.removeDOMEventListeners(tagObj.element);
               }
            }
            var props = serverTag == null || serverTag.props == null ? tagObj.eventAttNames : serverTag.props.concat(tagObj.eventAttNames);
            tagObj.listenerProps = props;
            tagObj.updateFromDOMElement(element);
         }
      }
      else if (id === "window") {
         syncMgr.initWindowSync(serverTag.props);
      }
      else if (id === "document") {
         syncMgr.initDocumentSync(serverTag.props);
      }
      else {
         if (tagObj != null) {
            if (js_Element_c.verbose)
               sc_log("Removing serverTag object " + id + ": no longer in DOM");
            sc_DynUtil_c.dispose(tagObj);
            tagObj = null;
         }
      }
      return tagObj;
   },
   addChange: function(obj, propName, val) {
      var id = obj.getId();
      var changeMap = null;
      if (id) {
         changeMap = syncMgr.changesByObjId[id];
         if (!changeMap) {
            changeMap = {};
            syncMgr.changesByObjId[id] = changeMap;
         }
      }
      else
         sc_logError("change missing id");
      var change = changeMap[propName];
      if (!change) {
         change = {o:obj, p:propName, v:val};
         syncMgr.pendingChanges.push(change);
         if (id) {
            changeMap[propName] = change;
         }
      }
      else
         change.v = val;

      var absDelay;
      var relDelay;
      // The sync delay to use has been overridden up the stack in an event handler for a specific component
      // use that delay for this event firing only. A value of -1 here means to disable the auto-sync for this
      // particular event, for example to implement a form field that waits till another button is pressed to sync.
      if (sc_ClientSyncManager_c.syncDelaySet) {
         if (sc_ClientSyncManager_c.currentSyncDelay == -1)
            return;
         absDelay = sc_ClientSyncManager_c.currentSyncDelay;
      }
      else {
         absDelay = sc_ClientSyncManager_c.syncMinDelay;
      }
      var nowTime = new Date().getTime();
      var timeSinceLastSend = (nowTime - sc_ClientSyncManager_c.lastSentTime);
      if (sc_ClientSyncManager_c.lastSentTime == -1 || timeSinceLastSend > absDelay)
         relDelay = 0;
      else
         relDelay = absDelay - timeSinceLastSend;
      syncMgr.scheduleSync(relDelay);
   },
   addMethReturn: function(res, callId) {
      var retType = res ? res.constructor.name : "null";
      syncMgr.pendingChanges.push({t:"methReturn", res:res, callId:callId, retType:retType});
      syncMgr.scheduleSync(sc_ClientSyncManager_c.syncMinDelay);
   },
   scheduleSync: function(delay) {
      sc_ClientSyncManager_c.lastSentTime = new Date().getTime();
      if (!syncMgr.syncScheduled) {
         syncMgr.syncScheduled = true;
         sc_addScheduledJob(syncMgr, syncMgr.sendSync, delay, false);
      }
   },
   sendSync: function() {
      if (syncMgr.numSendsInProgress >= syncMgr.maxSends) {
         sc_log("Not sending sync with: " + syncMgr.numSendsInProgress + " in progress due to maxSends reached");
         syncMgr.syncScheduled = false;
         syncMgr.pendingSync = true;
         return;
      }
      var changes = syncMgr.pendingChanges;
      var jsArr = [];

      sc_ClientSyncManager_c.lastSentTime = new Date().getTime();
      sc_log("Updating lastSentTime=" + sc_ClientSyncManager_c.lastSentTime);

      for (var i = 0; i < changes.length; i++) {
         var change = changes[i];

         var v = change.v;
         if (v != null) {
            if (v.constructor !== String && v.constructor !== Number && v.constructor !== Date && v.constructor !== Boolean) {
               var evName;
               var exProps = null;
               var idProps = null;
               if (v.constructor === Event)
                  evName = "Event";
               else if (v.constructor === MouseEvent) {
                  evName = "MouseEvent";
                  exProps = ["button", "clientX", "clientY", "screenX", "screenY", "altKey", "metaKey", "shiftKey", "ctrlKey"];
               }
               else if (v.constructor === KeyboardEvent) {
                  evName = "KeyboardEvent";
                  exProps = ["key", "repeat", "altKey", "metaKey", "shiftKey", "ctrlKey"];
               }
               else if (v.constructor === FocusEvent) {
                  evName = "FocusEvent";
                  idProps = ["relatedTarget"];
               }
               else if (v.constructor === SubmitEvent) {
                  evName = "SubmitEvent";
                  idProps = ["target"];
               }
               else {
                  sc_logError("Property: " + change.p + " no serializer for: " + v);
                  evName = null;
               }
               if (evName) { // For events, need to first create the event, then send a 'ref' to it
                  if (!v.currentTag) { // We should have an scObj for all events we are listening on
                     sc_logError("Unable to send event change - missing object");
                     continue;
                  }
                  jsArr.push({$pkg:"sc.lang.html"});
                  var evBaseId = evName + "__" + syncMgr.eventIndex;
                  var pp = "sc.lang.html.";
                  var evId = pp + evBaseId;
                  jsArr.push({$new: [evBaseId, pp + evName, null]});
                  var evDef = {};
                  var eval ={type: v.type, currentTag:"ref:" + v.currentTag.getId(), timeStamp:v.timeStamp};
                  var pi, p;
                  if (exProps) {
                     for (pi = 0; pi < exProps.length; pi++) {
                        p = exProps[pi];
                        eval[p] = v[p];
                     }
                  }
                  if (idProps) {
                     for (pi = 0; pi < idProps.length; pi++) {
                        p = idProps[pi];
                        var vp = v[p];
                        if (vp)
                           idProps[p] = "ref:" + vp.id;
                     }
                  }
                  evDef[evBaseId] = eval;
                  jsArr.push(evDef);
                  jsArr.push({$pkg:""});
                  v = "ref:" + evId;

                  syncMgr.eventIndex++;
               }
            }
         }

         // TODO: need to gather the 'initial' state at least - map stored by the 'id' of the object.  When
         // serverTags are removed, must remove that state.  Don't capture 'events' in the initial store since that could cause a 'replay' of a
         // form submit.  In fact, we need a way to mark the clickEvent etc. so they are not stored in the initial state on the server.
         var type = change.t;
         if (!type) { //
            // TODO: should we gather all consecutive changes for the same object - or will that even happen on the client?
            // Output the sync property format for a property change: { "objId" : { "propName" : val } }
            var propChange = {};
            propChange[change.p] = v;
            var objChange = {};
            objChange[change.o.getId()] = propChange;
            jsArr.push(objChange);
         }
         else if (type === "methReturn") {
            var mr = {$methReturn:change.res, callId:change.callId, retType:change.retType};
            jsArr.push(mr)
         }
      }

      var jObj = {sync:jsArr};
      var jStr = JSON.stringify(jObj, null, 3);
      syncMgr.writeToDestination(jStr, "");
      syncMgr.syncSequence++;

      syncMgr.pendingChanges = [];
      syncMgr.changesByObjId = {};
      // Allow another sync since this one has gone out
      syncMgr.syncScheduled = false;
   },
   writeToDestination:function(json,paramStr) {
      var url = "/sync?url=" + encodeURI(window.location.pathname) + "&windowId=" + sc_windowId + paramStr;

      var anyChanges = json.length !== 0;

      if (!anyChanges && syncMgr.waitTime !== -1) {
         url += "&waitTime=" + syncMgr.waitTime;
         syncMgr.numWaitsInProgress++;
         if (!syncMgr.exitListener) {
            syncMgr.exitListener = true;
            // Notifies the server this window is gone so it can close the sync connection. This is also one last time potentially to
            // send a little bit of sync state to the server since it will process the request, close any pending sync requests, etc.
            window.addEventListener("unload", function() {
               navigator.sendBeacon(url + "&close=true", "");
            }, false);
         }
      }
      else
         syncMgr.numSendsInProgress++;
      if (!anyChanges) {
         if (sc_SyncManager_c.trace)
            sc_log("Sending sync wait request: " + (syncMgr.waitTime === -1 ? "(no wait)" : "wait: " + syncMgr.waitTime));
      }
      else {
         if (sc_SyncManager_c.trace)
            sc_log("Sending sync: " + json);
      }
      syncMgr.pendingSends.push(json);
      sc_PTypeUtil_c.postHttpRequest(url, json, "text/plain", new sc_SyncListener(anyChanges));
   },
   autoSync:function() {
      if (syncMgr.pendingSends.length == 0) {
         sc_log("autoSync - writing to destination");
         syncMgr.writeToDestination("", "");
      }
      else
         sc_log("autoSync - not writing to destination - " + syncMgr.pendingSends.length + " pending requests - numSends: " + syncMgr.numSendsInProgress + " numWaits:" + syncMgr.numWaitsInProgress);
      syncMgr.autoSyncScheduled = false;
   },
   postCompleteSync:function() {
      if (syncMgr.syncDestination.realTime && syncMgr.pollTime !== -1 && syncMgr.pendingSends.length == 0 &&
          syncMgr.connected && !syncMgr.autoSyncScheduled && !syncMgr.syncScheduled) {
         sc_log("Post complete sync: scheduling autoSync with: " + syncMgr.pendingSends.length + " pending requests");
         syncMgr.autoSyncScheduled = true;
         setTimeout(syncMgr.autoSync, syncMgr.pollTime);
      }
      else if (!syncMgr.syncDestination.realTime) {
         sc_log("Post complete sync: realTime disabled");
      }
      else {
         if (syncMgr.autoSyncScheduled)
            sc_log("Post complete sync - auto sync already scheduled");
         else
            sc_log("Post complete sync: not scheduling autoSync with: " + syncMgr.pendingSends.length + " pending requests, syncScheduled: " + syncMgr.syncScheduled + ", connected: " + syncMgr.connected);
      }
   },
   // For readability in the logs, flexibility in code-gen and efficiency in rendering we send the start tag txt all at once so
   // there's work here in parsing it and updating the DOM.
   // NOTE: close replica in tags.js so keep these two in sync
   setStartTagTxt:function(elem, startTagTxt) {
      if (!startTagTxt.startsWith("<")) {
         sc_logError("invalid start tag txt");
         return;
      }

      var tagName = "";
      var tnix;
      var stlen = startTagTxt.length;
      var endTag = false;
      for (tnix = 1; tnix < stlen; tnix++) {
         var c = startTagTxt.charAt(tnix);
         if (/\s/.test(c))
            break;
         if (c == '/' || c == '>') {
            endTag = true;
            break;
         }
         tagName = tagName + c;
      }
      if (tagName == null) {
         sc_logError("Missing tag name");
         return;
      }
      if (tagName.toLowerCase() !== elem.tagName.toLowerCase()) {
         sc_logError("Invalid tag name change - current tag: " + elem.tagName + " != " + tagName);
         return;
      }
      var attName = "";
      var oldAtts = elem.attributes;
      var newAtts = {};
      var newAttsArr = [];
      var anix = tnix + 1;
      for (; anix < stlen; anix++) {
         var c = startTagTxt.charAt(anix);
         if (c == '=') { // parse attribute
            if (attName == "") {
               sc_logError("Invalid attribute");
               return;
            }
            var avix = anix + 1;
            var attVal = "";
            var delim = null;
            for (; avix < stlen; avix++) {
               c = startTagTxt.charAt(avix);
               if (!delim) {
                  if (c == '"' || c == "'") {
                     if (attVal != "") {
                        sc_logError("Invalid attribute");
                        return;
                     }
                     delim = c;
                  }
                  else if (/\s/.test(c)) {
                     if (attVal == "")
                        continue;
                     else
                        break;
                  }
                  else if (c == '>' || c == '/') {
                     endTag = true;
                     break;
                  }
                  else {
                     attVal += c;
                  }
               }
               else {
                  if (delim == c) {
                     delim = null;
                     break;
                  }
                  if (avix < stlen - 1) {
                     if (c == '\\') {
                        avix++;
                        var nc = startTagTxt.charAt(avix);
                        if (nc == '"' || nc == '\'' || nc == '\\')
                           c = nc;
                        else {
                           var esc = true;
                           if (nc == 'n') {
                              attVal += '\n';
                           }
                           else if (nc == 't') {
                              attVal += '\t';
                           }
                           else if (nc == 'r') {
                              attVal += '\r';
                           }
                           else {
                              sc_logError("Unrecognized escape");
                              return;
                           }
                           continue;
                        }
                     }
                     else if (c == '&') {
                        var lix = startTagTxt.indexOf(';', avix);
                        if (lix != -1) {
                           var ln = startTagTxt.substring(avix+1, lix);
                           if (ln === "quot") {
                              avix += 5;
                              c = '"';
                           }
                           else if (ln === "amp") {
                              avix += 4;
                              c = '&';
                           }
                        }
                     }
                  }
                  attVal += c;
               }
            }
            if (delim != null) {
               sc_logError("Unclosed string with: " + delim);
               return;
            }
            attName = attName.toLowerCase();
            newAtts[attName] = attVal;
            newAttsArr.push(attName);
            attName = "";
            attVal = "";
            anix = avix;
         }
         else if (c == '/' || c == '>') {
            endTag = true;
            break;
         }
         else if (/\s/.test(c)) {
            if (attName == "")
               continue;
            else {
               newAtts[attName] = true;
               newAttsArr.push(attName);
               attName = "";
            }
         }
         else {
            attName += c;
         }
         if (endTag)
            break;
      }
      if (!endTag) {
         sc_logError("Invalid start tag txt");
         return;
      }
      if (attName.length > 0) { // last value-less attribute
         newAtts[attName] = true;
         newAttsArr.push(attName);
      }
      var isInput = tagName === "input";
      for (var i = 0; i < newAttsArr.length; i++) {
         var newAttName = newAttsArr[i];
         var oldVal = isInput && newAttName === "value" ? elem.value : elem.getAttribute(newAttName);
         var newVal = newAtts[newAttName];
         if (oldVal == null) {
            if (newVal === true && elem.hasAttribute(newAttName))
               elem.removeAttribute(newAttName);
         }
         if (oldVal == null || oldVal != newVal) {
            if (isInput && newAttName === "value") {
               elem.value = newVal;
               if (elem.scObj)
                  elem.scObj.value = newVal;
            }
            else
               elem.setAttribute(newAttName, newVal);
         }
      }
      for (var i = 0; i < oldAtts.length; i++) {
         var oldAttName = oldAtts[i].name;
         if (!newAtts[oldAttName]) {
            elem.removeAttribute(oldAttName);
         }
      }
   },
   schedRefreshTags:function() {
      if (!syncMgr.refreshTagsScheduled) {
         syncMgr.refreshTagsScheduled = true;
         setTimeout(syncMgr.refreshTags, 5);
      }
   },
   beginSync:function() {},
   endSync:function(){},
   documentTag:new js_Document(),
   windowWrap:{
      getId:function() { return "window"; }
   },
   sendWindowSizeEvents:function(wp) {
      for (var i = 0; i < wp.length; i++) {
         sc_Bind_c.sendChangedEvent(syncMgr.windowWrap, wp[i] , window[wp[i]]);
      }
   },
   initDocumentSync:function(props) {
      var first = !syncMgr.documentSyncProps;
      var np = sc_updatePlist("documentSyncProps", props);
      if (np.length) {
         for (var i = 0; i < np.length; i++) {
            var propName = np[i];
            var listener = domEvents[propName];
            if (listener.callback == null) {
               js_HTMLElement_c.initDOMListener(listener, propName, null);
            }
            sc_addEventListener(document, listener.eventName, listener.callback);
         }
      }
   },
   initWindowSync:function(props) {
      var first = !syncMgr.windowSyncProps;
      var np = sc_updatePlist("windowSyncProps", props);
      if (np.length) {
         syncMgr.sendWindowSizeEvents(np); // send changes for new properties right away since they are not on the server
         if (first) { // first time need to add the listener
            sc_addEventListener(window, "resize", function(event) {
               syncMgr.sendWindowSizeEvents(syncMgr.windowSyncProps);
            });
         }
      }
   }
};

sc_ClientSyncManager_c = {defaultRealTime: true, syncDelaySet:false, currentSyncDelay:-1, syncMinDelay:100, lastSentTime:-1};


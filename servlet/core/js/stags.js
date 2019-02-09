/**
* stags.js: implements 'serverTags only' Javascript support.
* This is a light version of tags.js, sharing code + structure.  Used when there is no Java code on the client for an schtml server page.
* Because there's no Java code converted to JS, this page has less to do than tags.js and eliminates many signficiant dependencies (e.g. javasys.js).
* It uses the same 'sync' protocol as the full JS runtime, including managing the 'serverTags' list received from the server.
* That includes the list of DOM elements with event listeners on the server.  For each server tag, we'll create a corresponding tagObject and add listeners for those DOM events.  When the DOM events fire, a change
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
var sc$nextid = 1;
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

function sc_hasProp(obj, prop) {
   if (obj.hasOwnProperty)
      return obj.hasOwnProperty(prop);
   return obj[prop] !== undefined;
}

function sc_refresh() { // Called at the end of loading a page - in case autoSync is turned on, kick off the first autoSync
   sc_log("sc_refresh() called");
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
   sc_rlog(str);
   console.log(str);
}

function sc_getConsoleLog() {
   if (window.sc_consoleLog)
      return window.sc_consoleLog.join("\n");
   return "<empty js console>";
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
            if (stat != 205) // This is the sync reset response
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
               sc_logError("Return status: " + stat + " for: " + url);
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
js_HTMLElement_c.domEvents = {clickEvent:{}, dblClickEvent:{}, mouseDownEvent:{}, mouseMoveEvent:{}, mouseOverEvent:{aliases:["hovered"], computed:true}, mouseOutEvent:{aliases:["hovered"], computed:true}, mouseUpEvent:{}, keyDownEvent:{}, keyPressEvent:{}, keyUpEvent:{}, submitEvent:{}, changeEvent:{}, blurEvent:{}, focusEvent:{}, resizeEvent:{aliases:["innerWidth","innerHeight"]}};

// the reverse direction for the aliases field of the domEvent entry
js_HTMLElement_c.domAliases = {innerWidth:"resizeEvent", innerHeight:"resizeEvent", hovered:["mouseOverEvent","mouseOutEvent"]};
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
   js_HTMLElement_c.processEvent(event.currentTarget, event, listener);
};

js_HTMLElement_c.processEvent = function(elem, event, listener) {
   var scObj = elem.scObj;
   if (scObj !== undefined) {

      // Add this as a separate field so we can use the exposed parts of the DOM api from Java consistently
      event.currentTag = scObj;
      var eventValue;

      if (listener.alias != null) {
         // e.g. innerWidth or hovered - properties computed from other DOM events
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

      if (js_Element_c.trace && listener.scEventName != "mouseMoveEvent")
         sc_log("tag event: " + listener.propName + ": " + listener.scEventName + " = " + eventValue);
      sc_Bind_c.sendChangedEvent(scObj, listener.propName, eventValue);

      // TODO: for event properties should we delete the property here or set it to null?  flush the queue of events if somehow a queue is enabled here?
   }
   else
      sc_log("Unable to find scObject to update in eventHandler");
};

js_HTMLElement_c.getInnerWidth = function() {
   if (this.element == null)
      return 0;
   return this.element.clientWidth;
};

js_HTMLElement_c.getInnerHeight = function() {
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
            else // Now that we know the value of the aliased property (e.g. innerHeight) we need to send a change event cause it changes once we have an element.
               sc_Bind_c.sendChangedEvent(this, listener.propName, sc_DynUtil_c.getPropertyValue(this, listener.propName));

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

js_HTMLElement_c.initDOMListener = function(listener, prop, scEventName) {
   if (scEventName == null)
      scEventName = prop;
   else
      listener.alias = true; // This is like innerWidth which is mapped to a separate resizeEvent
   // Convert from the sc event name, e.g. clickEvent to click
   listener.eventName = scEventName.substring(0, scEventName.indexOf("Event")).toLowerCase();
   listener.scEventName = scEventName;
   listener.propName = prop;
   listener.callback = sc_newEventArgListener(js_HTMLElement_c.eventHandler, listener);
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

// For the given HTMLElement instance, return the array of dom event listeners (dom listeners).  The domEvents like clickEvent, are converted to properties
// of the tag object.  We do this lazily in the browser for speed, and only create and set the properties for the events which someone is listening to.
// This means we do need to listen for the 'addListener' binding event so when a new listener is added, we can start listening on the appropriate dom
// DOM event property.
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

js_HTMLElement_c.destroy = function() {
   var origElem = this.element;
   this.element = null;
   this.domChanged(origElem, null);
}

function js_Input() {
   js_HTMLElement.call(this);
}
js_Input_c = sc_newClass("Input", js_Input, js_HTMLElement);
js_Input_c.eventAttNames = js_HTMLElement_c.eventAttNames.concat(["value", "checked", "changeEvent", "clickCount"]);
js_Input_c.removeOnEmpty = {value:true};

js_Input_c.domChanged = function(origElem, newElem) {
   if (this.type == "button") {
      js_Button_c.domChanged.call(this, origElem, newElem);
      return;
   }
   js_HTMLElement_c.domChanged.call(this, origElem, newElem);
   if (origElem !== null) {
      sc_removeEventListener(origElem, 'change', js_Input_c.doChangeEvent);
      sc_removeEventListener(origElem, 'keyup', js_Input_c.doChangeEvent);
   }
   if (newElem !== null) {
      sc_addEventListener(newElem, 'change', js_Input_c.doChangeEvent);
      sc_addEventListener(newElem, 'keyup', js_Input_c.doChangeEvent);
   }
}

js_Input_c.updateFromDOMElement = function(newElem) {
   js_HTMLElement_c.updateFromDOMElement.call(this, newElem);
   this.value = newElem.value;
   this.checked = newElem.checked;
}

js_Input_c.doChangeEvent = function(event) {
   var elem = event.currentTarget ? event.currentTarget : event.srcElement;
   var scObj = elem.scObj;
   if (scObj !== undefined) {
      if (scObj.setValue) {
         scObj.setValue(this.value);
         if (this.value === "" && scObj.removeOnEmpty.value != null)
            this.removeAttribute("value");
      }
      if (scObj.setChecked)
         scObj.setChecked(this.checked);
   }
   else
      sc_log("Unable to find scObject to update in doChangeEvent");
}

js_Input_c.setValue = function(newVal) {
   if (newVal != this.value) {
      this.value = newVal;
      if (this.element !== null && this.element.value != newVal)
         this.element.value = newVal;
      sc_Bind_c.sendChangedEvent(this, "value" , newVal);
   }
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
js_Select_c.eventAttNames = js_HTMLElement_c.eventAttNames.concat([ "selectedIndex"]);

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
   js_HTMLElement_c.domChanged.call(this, origElem, newElem);
   if (origElem != null)
      sc_removeEventListener(origElem, 'change', js_Select_c.doChangeEvent);
   if (newElem != null) {
      sc_addEventListener(newElem, 'change', js_Select_c.doChangeEvent);
   }
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

function js_Form() {
   js_HTMLElement.call(this);
   this.submitCount = 0;
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
   var evt = new Event('submit');
   evt["currentTarget"] = evt["target"] = this; // not using "." here because intelliJ complains these are constant - will any browsers barf on this?  If so we'll need to just create a new object and copy over any fields we need.
   this.submitEvent = evt;
   sc_Bind_c.sendChangedEvent(this, "submitEvent", evt);
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

function js_Option() {
   js_HTMLElement.call(this);
   this.optionData = null;
   this.selected = false;
}
js_Option_c = sc_newClass("Option", js_Option, js_HTMLElement);
js_Option_c.eventAttNames = js_HTMLElement_c.eventAttNames.concat["selected", "optionData"];

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
   return js_RepeatTag_c.getRepeatInnerName(id) != null;
}

js_RepeatTag_c.getRepeatInnerName = function(id) {
   // Check if this is a 'repeat' element based on it's id name scheme and compute the id prefix for it's children
   var ix = id.indexOf('_Repeat');
   var repeatInnerName = null;
   if (ix != -1) {
      var rlen = '_Repeat'.length
      if (ix + rlen == id.length)
         repeatInnerName = id.substring(0, ix);
      else if (id.charAt(ix+rlen) == '_') // TODO: if it is like foo_repeat_3 we need to do foo_3 right?
         repeatInnerName = id.substring(0, ix) + id.substring(ix + rlen);
   }
   return repeatInnerName;
}

js_HTMLElement_c.tagNameToType = {input:js_Input, form:js_Form, select:js_Select, option:js_Option};
js_ServerTag_c = {
   equals: function(s1,s2) {
      return s1 === s2 || (s1 != null && s2 != null && s1.id === s2.id); // TODO: check if props have changed
   }
};

function sc_SyncListener(anyChanges) {
   this.anyChanges = anyChanges;
}

sc_SyncListener_c = sc_newClass("SyncListener", sc_SyncListener, null);

sc_SyncListener_c.completeSync = function() {
   if (this.anyChanges) {
      syncMgr.numSendsInProgress--;
   }
   else {
      syncMgr.numWaitsInProgress--;
   }
}

sc_SyncListener_c.response = function(responseText) {
   sc_log("in response handler");
   this.completeSync();
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
                     syncMgr.applySyncLayer("json", processDef, this.anyChanges ? "send" : "wait");
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
   this.completeSync();
   if (code === 205) { // session on server has expired
      // TODO: need to do initial sync code here.  We'll collect all of the changes we want the server to have
      // - writeToDestination(initSyncJSON, null, this, "reset=true", null);
      // accumulating probably input.value, selectedIndex, etc. but not changeEvent.  Send that to the server
      // part of the reset (SyncDestination.SyncListener)
      sc_logError("*** Server session - expired - reset not yet implemented for server-only client");
   }
   else if (code === 410) { // server shutdown
      syncMgr.connected = false;
      syncMgr.realTime = false;
      sc_logError("*** Server shutdown");
   }
   else if (code === 500 || code === 0) {
      syncMgr.connected = false;
      sc_logError("*** Server reported error code: " + code);
   }
   else
      sc_logError("*** Unrecognized server error: " + code);
   syncMgr.postCompleteSync();
};

syncMgr = sc_SyncManager_c = {
   trace: false,
   serverTags:{},
   serverTagList:[],
   tagObjects:{},
   pendingChanges:[],
   syncScheduled:false,
   eventIndex:0,
   realTime:true,
   waitTime:1200000, // TODO: make this configurable in the page or URL?
   pollTime: 500,
   numSendsInProgress: 0,
   numWaitsInProgress: 0,
   autoSyncScheduled: false,
   connected: true,
   refreshTagsScheduled: false,
   applySyncLayer: function(lang,json,detail) {
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
      var activeServerTags = {}; // when the list of server tags has changed, contains the new serverTags
      var newServerTagList = [];
      var serverTagsChanged = false; // has the list of serverTags changed
      var needsTagRefresh = false; // has the body of any element changed which might contain serverTags requiring a refresh
      for (var i = 0; i < sl.length; i++) {
         var cmd = sl[i];
         var newArgs = cmd["$new"];
         if (newArgs) {
            var cl = newArgs[1];
            if (cl === "sc.js.ServerTag")
               newServerTags[newArgs[0]] = {name:newArgs[0]};
            // These map to system objects so no server tag instance
            else if (cl !== "sc.js.ServerTagManager" && cl !== "sc.lang.html.Location")
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
            if (name.startsWith("ServerTag__")) {
               var st = newServerTags[name];
               if (!st)
                  st = syncMgr.serverTags[name];
               var stProps = cmd[name];
               if (st) {
                  st.id = stProps.id; 
                  st.props = stProps.props;
               }
               else
                  sc_logError("No ServerTag for modify");
            }
            else if (name.startsWith("Location__")) {
               var lps = cmd[name];
               var pnames = Object.keys(lps);
               for (var i = 0; i < pnames.length; i++) {
                  var pname = pnames[i];
                  window.location[pname] = lps[pname];
               }
            }
            // e.g. { "PageServerTagManager":{
            //       "serverTags":{"a":"ref:ServerTag__0","input":"ref:ServerTag__1","input_1":"ref:ServerTag__2","form":"ref:ServerTag__4",..."}
            else if (name === "PageServerTagManager") {
               // Some changes being made to the server tag manager.  Now, when any changes are made, we send the entire new list
               // so that means we can clean up old tag objects associated with server tags we no longer need to listen (or that may no longer exist)
               serverTagsChanged = true;
               var stObj = cmd[name].serverTags;
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
                        activeServerTags[stId] = refServerTag;
                     }
                  }
                  else
                     sc_logError("Invalid serverTag ref");
               }
            }
            else if (name === "DynUtil") { // Support some select remote methods for testing
               var mObj = cmd[name];
               var mName = mObj.$meth;
               var callId = mObj.callId;
               if (mName === "evalScript") {
                  var res = eval(mObj.args[0]);
                  var retType = res ? res.constructor.name : "null";
                  syncMgr.pendingChanges.push({t:"methReturn", res:res, callId:callId, retType:retType});
                  syncMgr.scheduleSync();
               }
            }
            else {
               var chElem = document.getElementById(name);
               if (chElem) {
                  var chObj = cmd[name];
                  var props = Object.keys(chObj);
                  for (var pix = 0; pix < props.length; pix++) {
                     var prop = props[pix];
                     var val = chObj[prop];
                     if (prop === "startTagTxt") {
                        syncMgr.setStartTagTxt(chElem, val);
                     }
                     else if (prop === "innerHTML") {
                        var repeatInnerName = js_RepeatTag_c.getRepeatInnerName(name);
                        if (repeatInnerName != null) { // repeat tag needs special processing - remove all 'repeat children', then insert the innerHTML after the marker element
                           do {
                              var next = chElem.nextElementSibling;
                              if (next == null)
                                 break;
                              var nextId = next.id;
                              if (nextId == null)
                                 break;
                              var ix = nextId.indexOf(repeatInnerName);
                              // continue removing elements while the nextId is either == name or name_1, name_2, etc.
                              if (ix !== 0 || (nextId.length !== repeatInnerName.length && !/_\d/.test(nextId.substr(repeatInnerName.length))))
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
                        // ignoring since we receive the value in startTagTxt as well
                        chElem.value = val;
                     }
                     else if (prop === "checked")
                        chElem.checked = val;
                     else if (prop === "selectedIndex")
                        chElem.selectedIndex = val;
                     else if (prop === "style")
                        chElem.style = val;
                     else
                        sc_logError("Unrecognized property in stags sync layer: " + prop);
                  }
               }
               else
                  sc_logError("Unable to find DOM element to match id: " + name);
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
            if (!activeServerTags[oldSt.name]) {
               var oldTagObj = tagObjects[id];
               if (oldTagObj != null) {
                  delete tagObjects[id];
                  oldTagObj.destroy();
               }
            }
         }
         syncMgr.serverTags = activeServerTags;
         syncMgr.serverTagList = newServerTagList;
      }
      else if (needsTagRefresh)
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
      syncMgr.pendingChanges.push({o:obj, p:propName, v:val});
      syncMgr.scheduleSync();
   },
   scheduleSync: function() {
      if (!syncMgr.syncScheduled) {
         syncMgr.syncScheduled = true;
         sc_addRunLaterMethod(syncMgr, syncMgr.sendSync, 5);
      }
   },
   sendSync: function() {
      var changes = syncMgr.pendingChanges;
      var jsArr = [];

      for (var i = 0; i < changes.length; i++) {
         var change = changes[i];

         var v = change.v;
         if (v != null) {
            if (v.constructor !== String && v.constructor !== Number && v.constructor !== Date && v.constructor !== Boolean) {
               var evName;
               if (v.constructor === Event)
                  evName = "Event";
               else if (v.constructor === MouseEvent)
                  evName = "MouseEvent";
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
                  evDef[evBaseId] = {type: v.type, currentTag:"ref:" + v.currentTag.getId(), timeStamp:v.timeStamp};
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
      syncMgr.writeToDestination(jStr);

      syncMgr.pendingChanges = [];
      // Allow another sync since this one has gone out
      syncMgr.syncScheduled = false; // TODO: add configuration option to allow more than one send at a time (and set this in the SyncListener response/error instead)
   },
   writeToDestination:function(json) {
      var url = "/sync?url=" + encodeURI(window.location.pathname) + "&windowId=" + sc_windowId;

      var anyChanges = json.length !== 0;

      if (!anyChanges && syncMgr.waitTime !== -1) {
         url += "&waitTime=" + syncMgr.waitTime;
      }
      if (!anyChanges) {
         syncMgr.numWaitsInProgress++;
         if (sc_SyncManager_c.trace)
            sc_log("Sending sync wait request: " + (syncMgr.waitTime === -1 ? "(no wait)" : "wait: " + syncMgr.waitTime));
      }
      else {
         syncMgr.numSendsInProgress++;
         if (sc_SyncManager_c.trace)
            sc_log("Sending sync: " + json);
      }

      sc_PTypeUtil_c.postHttpRequest(url, json, "text/plain", new sc_SyncListener(anyChanges));
   },
   autoSync:function() {
      if (syncMgr.numSendsInProgress === 0 && syncMgr.numWaitsInProgress === 0) {
         sc_log("autoSync - writing to destination");
         syncMgr.writeToDestination("");
      }
      else
         sc_log("autoSync - not writing to destination");
      syncMgr.autoSyncScheduled = false;
   },
   postCompleteSync:function() {
      if (syncMgr.pollTime !== -1 && syncMgr.numSendsInProgress === 0 && syncMgr.numWaitsInProgress === 0 && syncMgr.connected && !syncMgr.autoSyncScheduled) {
         sc_log("Post complete sync: scheduling autoSync with:  numSends: " + syncMgr.numSendsInProgress + " numWaits: " + syncMgr.numWaitsInProgress)
         syncMgr.autoSyncScheduled = true;
         setTimeout(syncMgr.autoSync, syncMgr.pollTime);
      }
      else
         sc_log("Post complete sync: not scheduling autoSync with:  numSends: " + syncMgr.numSendsInProgress + " numWaits: " + syncMgr.numWaitsInProgress)
   },
   // For readability in the logs, flexibility in code-gen and efficiency in rendering we send the start tag txt all at once so
   // there's work here in parsing it and updating the DOM.
   // NOTE: replicated in tags.js so keep these two in sync
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
                  if (c == '\\' && avix < stlen - 1) {
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
            if (isInput && newAttName === "value")
               elem.value = newVal;
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
   endSync:function(){}
};

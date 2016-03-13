// You can use this type directly and get the basic functionality for any HTML tag but typically it is
// used by generated schtml templates.
//
// js_HTMLElement is the base type for the js class which attaches to and controls the DOM element.  
// It implements change functionality, events bindings, repeat, refresh, etc.
// Subclasses include js_Page, js_Html, js_Body, etc. which can add data binding or other behavior for
// specific attributes.  
function js_HTMLElement() {
   this.bodyValid = true;
   this.startValid = true;
   this.repeatTagsValid = true;
   this.tagName = null;
   this.element = null;
   this.id = null;
   this.repeat = null;
   this.repeatVar = null;
   this.repeatIndex = -1;
   this.repeatVarName = null;
   this.HTMLClass = null;
   this.visible = true;
   this.pendingEvent = null;
   this.pendingType = null;
}


js_Element_c = js_HTMLElement_c = sc_newClass("js_HTMLElement", js_HTMLElement, null, [sc_ISyncInit]);

// This is part of the SemanticNode class on the server and so the component code gen uses it even for client code
// involving component types which extend Element.  Just noop it here.
js_Element_c.init = js_Element_c.start = js_Element_c.stop = function() {};
js_Element_c.refreshStartTags = [];
js_Element_c.refreshBodyTags = [];
js_Element_c.refreshRepeatTags = [];
js_Element_c.refreshScheduled = false;
js_Element_c.globalRefreshScheduled = false;
js_Element_c.trace = false;
js_Element_c.verbose = false;
js_Element_c.needsRefresh = false;
js_Element_c.getURLPaths = function() {
   return [];
}

js_Element_c.doRefreshTags = function(tagList, startName, validName) {
   for (var i = 0; i < tagList.length; i++) {
      var tag = tagList[i];
      if (!tag[validName])
         tag[startName]();
   }
}

// Called when the type is modified 
js_Element_c._updateInst = function() {
   this.invalidate();
}

js_Element_c.refreshCount = 0;
js_Element_c.refreshTags = function() {
   try {
      var toRefresh = js_Element_c.refreshBodyTags;
      js_Element_c.refreshBodyTags = [];
      // TODO: could optimize this by sorting or removing child nodes whose parents are in the refresh list.  If we do refresh a higher level item before a lower level one, the child validate call at least should not happpen since we validate it already.
      js_Element_c.doRefreshTags(toRefresh, "refreshBody", "bodyValid");
      toRefresh = js_Element_c.refreshStartTags;
      js_Element_c.refreshStartTags = [];
      js_Element_c.doRefreshTags(toRefresh, "refreshStart", "startValid");
      toRefresh = js_Element_c.refreshRepeatTags;
      js_Element_c.refreshRepeatTags = [];
      js_Element_c.doRefreshTags(toRefresh, "refreshRepeat", "repeatTagsValid");

      if (js_Element_c.refreshBodyTags.length > 0 ||
          js_Element_c.refreshStartTags.length > 0 ||
          js_Element_c.refreshRepeatTags.length > 0) {
         if (js_Element_c.refreshCount == 15)
            console.error("Skipping recursive refreshes after 15 levels of processing - elements may not be rendered");
         else {
            js_Element_c.refreshCount++;
            try {
               js_Element_c.refreshTags();
            }
            finally {
              js_Element_c.refreshCount--;
            }
         }
      }
   }
   finally {
      js_Element_c.refreshScheduled = false;
   }
}

js_Element_c.getObjChildren = function(create) {
   if (this.repeatTags != null)
      return this.repeatTags;
   return null;
}

js_Element_c.escAtt = function(input) {
   if (input == null)
      return null;
   return input
         .replace(/"/g, "&quot;")
         .replace(/'/g, "&#039;");
}

js_Element_c.escBody = function(input) {
   if (input == null)
      return null;
    return input.toString()
         .replace(/&/g, "&amp;")
         .replace(/</g, "&lt;")
         .replace(/>/g, "&gt;")
         .replace(/"/g, "&quot;")
         .replace(/'/g, "&#039;");
}

js_HTMLElement_c.toString = function() {
   var id = this.getId();
   var tn = this.tagName;
   return "<" + (tn == null ? "element" : tn) + (id == null || id == "" ? "" : " id=\"" + id + "\"") + ">";
}

js_HTMLElement_c.setId = function(newVal) {
   this.id = newVal;
}

js_HTMLElement_c.setParentNode = function(parent) {
   this.parentNode = parent;
}

js_HTMLElement_c.getParentNode = function() {
   return this.parentNode;
}

js_HTMLElement_c.getPreviousElementSibling = function() {
   var elem = this.element;
   if (elem == null)
      return null;
   elem = elem.previousElementSibling;
   if (elem == null)
      return null;
   if (elem.scObj != null)
      return elem.scObj; // Need to return the tag object associated with the element if there is one.
   return null; 
}

js_HTMLElement_c.setVisible = function(vis) {
   if (vis != this.visible) {
      this.visible = vis;
      this.invalidate();
      sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "visible" , vis);
   }
}

js_HTMLElement_c.getVisible = function() {
   return this.visible;
}

js_HTMLElement_c.setHTMLClass = function(cl) {
   if (cl != this.HTMLClass) {
      this.HTMLClass = cl; 
      if (this.element !== null && this.element.getAttribute("class") != cl)
         this.element.setAttribute("class", cl);
      sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "HTMLClass" , cl);
   }
}

js_HTMLElement_c.getHTMLClass = function() {
   return this.HTMLClass;
}

js_HTMLElement_c.setStyle = function(st) {
   if (st != this.style) {
      this.style = st; 
      var el = this.element;
      //if (el !== null && el.style != st)
      //   el.style = st;

      if (el !== null && el.getAttribute("style") != st)
         el.setAttribute("style", st);
      sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "style" , st);
   }
}

js_HTMLElement_c.getHTMLClass = function() {
   return this.HTMLClass;
}

// On the server, this returns the set of JSFiles required to load the schtml file (or all files).  They are called from the script elements that typically run on the server.
// We don't need to generate the script elements on the client so return null here.
js_HTMLElement_c.getJSFiles = js_HTMLElement_c.getAllJSFiles = function() {
   return null;
}

js_HTMLElement_c.destroyRepeatTags = function() {
   var repeatTags = this.repeatTags;
   if (repeatTags !== null) {
      for (var i = 0; i < repeatTags.length; i++) {
         var childTag = repeatTags[i];
         childTag.destroy();
      }
      this.repeatTags = null;
   }
}

js_HTMLElement_c.removeFromDOM = function() {
   var curElem = this.element;
   if (curElem == null || this.parentNode == null || this.parentNode.element == null)
      return false;
   try {
      this.parentNode.element.removeChild(curElem);
   }
   catch (e) {
      console.error("removeFromDOM for tag: " + this.id + " did not find a DOM element to remove even though this tag had one set: " + a);
   }
   this.setDOMElement(null);
   this.startValid = this.bodyValid = true;
   return true;
}

/*
js_HTMLElement_c.insertIntoDOM = function() {
   this.updateDOM(false, false);
   var curElem = this.element;
   if (curElem != null)
      return;
   var parent = this.parentNode;
   if (parent == null || parent.element == null)
      return false;
   var children = parent.getObjChildren();
   if (children == null)
      return false;
   var ix = children.indexOf(this);
   if (ix == -1) 
      return false; // Refresh the parent?

   var tmp = document.createElement('div');
   tmp.innerHTML = this.output().toString();

   var nextTag = null;
   // See if there's a next element in the DOM tree for any of our following children
   ix++;
   while (ix < children.length) {
      nextTag = children[ix];
      if (nextTag.element != null) {
         break;
      }
      ix++;
   }
   parent.element.insertBefore(tmp.childNodes[0], nextTag);
   this.startValid = this.bodyValid = true;
   // TODO: do we need to remove the temporary div we created?  I can't find out how to do that... just get errors
   return true;
}
*/

js_HTMLElement_c.destroy = function() {
   var origElem = this.element;
   this.element = null;
   this.domChanged(origElem, null);
   this.removeAttributeListener();
   if (this.repeat != null) {
      this.destroyRepeatTags();
   }
   if (this.repeatListener !== undefined) {
      sc_Bind_c.removeListener(this, "repeat", this.repeatListener, sc_IListener_c.VALUE_VALIDATED);
      delete this.repeatListener;
   }
   if (this.getObjChildren) {
      var children = this.getObjChildren(false);
      if (children != null) {
         for (var i = 0; i < children.length; i++) {
            var child = children[i];
            if (child != null && sc_instanceOf(child, js_HTMLElement)) {
               child.destroy();
            }
         }
      }
   }
   sc_DynUtil_c.dispose(this, false);
}

// Some Java property names conflict with DOM attribute names, e.g. 'class'  This table provides a global way to workaround these name-space conflicts.
js_HTMLElement_c.propertyAttributeAliases = {"htmlClass":"class"};
js_HTMLElement_c.attributePropertyAliases = {"class":"htmlClass"};

js_HTMLElement_c.mapPropertyToAttribute = function(name) {
   var res = js_HTMLElement_c.propertyAttributeAliases[name];
   if (res === undefined)
      return name;
   return res;
}

js_HTMLElement_c.mapAttributeToProperty = function(name) {
   var res = js_HTMLElement_c.attributePropertyAliases[name];
   if (res === undefined)
      return name;
   return res;
}

// Marks the names of object properties that are copied back into the DOM for this tag
js_HTMLElement_c.refreshAttNames = ["class", "style", "repeat"];
// The set of attributes when their value goes to null or "" the attribute name itself is removed
js_HTMLElement_c.removeOnEmpty = {};

var sc$rootTags = new Object();
var sc$rootTagsArray = [];

function sc_refresh() {
   for (var i = 0; i < sc$rootTagsArray.length; i++) {
      var rootTag = sc$rootTagsArray[i];
      if (rootTag.needsRefresh) {
         sc_Bind_c.refreshBindings(rootTag);
      }
   }
}

var sc$idSpaces = {};
js_HTMLElement_c.allocUniqueId = function(baseName) {
   // 2nd param decides if we are specific or not to the client.  in that
   // case, initialize it with it's unique id space and suffix so the name
   // spaces don't collide
   var suffix = arguments.length > 1 && arguments[1] ? (this.serverContent ? "_s" : "_c") : null;
   if (suffix != null)
      baseName = baseName + suffix;
   var nextId = sc$idSpaces[baseName];
   if (nextId == null) {
      sc$idSpaces[baseName] = 1;
      return baseName;
   }
   sc$idSpaces[baseName] = nextId + 1;
   // Need a separator here.  The baseName is made unique within its parent with a simple number suffix.  This extra number identifies the nth instance of that sub-tag.
   return baseName + (suffix == null ? "_" : "") + nextId;
}

js_HTMLElement_c.updateDOM = function() {
   var newElement = null;
   if (this.repeat === null || this.repeat === undefined) {
      if (this.id == null) {
         var tname = this.tagName;
         if (tname != null) {
            // TODO: should we support modes for append, replace, before/after, etc.
            var elems = document.getElementsByTagName(tname);
            if (elems !== null && elems.length > 0)
               newElement = elems[0];
         }
      }
      else {
         newElement = document.getElementById(this.id);
      }
      this.setDOMElement(newElement);
   }
   else {
      var repeatTags = this.repeatTags;
      if (repeatTags !== null) {
         for (var i = 0; i < repeatTags.length; i++) {
            var childTag = repeatTags[i];
            childTag.updateDOM();
         }
      }
   }
   /*
   if (setStartValid)
      this.startValid = true;
   if (setBodyValid)
      this.bodyValid = true;
   */
}

js_HTMLElement_c.setDOMElement = function(newElement) {
   if (newElement !== this.element) {
      var orig = this.element;
      if (orig !== null) {
          delete orig.scObj;
      }

      if (this.element === null && newElement !== null) {
          this.addAttributeListener();
      }

      this.element = newElement;
      if (newElement !== null) {

               
         //sc_id(newElement); enable for debugging to make it easier to identify unique elements
        
         // This can happen if 
         if (newElement.scObj !== undefined) {
             console.log("Warning: replacing object: " + sc_DynUtil_c.getInstanceId(newElement.scObj) + " with: " + sc_DynUtil_c.getInstanceId(this) + " for tag: " + this.tagName); 
         }
         newElement.scObj = this;
      }
      this.domChanged(orig, newElement);
      this.updateChildDOMs();
   }

}

js_HTMLElement_c.addAttributeListener = function() {
   if (this._attListener !== undefined)
      return;

   var attNames = this.refreshAttNames;
   var domEvents = this.domEvents;
   if (attNames !== null || domEvents != null) {
      var listener = new js_RefreshAttributeListener(this);
      this._attListener = listener;
      if (attNames != null) {
         for (var i = 0; i < attNames.length; i++) {
            sc_Bind_c.addListener(this, js_HTMLElement_c.mapAttributeToProperty(attNames[i]), listener, sc_IListener_c.VALUE_VALIDATED);
         }
      }
      if (domEvents != null) {
         // Need to be notified when new listeners to this item are added.  If they happen to be domEvent listeners, we lazily register for those DOM events.
         sc_Bind_c.addListener(this, null, listener, sc_IListener_c.LISTENER_ADDED);
      }
   }
}

js_HTMLElement_c.removeAttributeListener = function() {
   var listener = this._attListener;
   if (listener !== undefined) {
      var attNames = this.refreshAttNames;
      if (attNames != null) {
         for (var i = 0; i < attNames.length; i++) {
            sc_Bind_c.removeListener(this, js_HTMLElement_c.mapAttributeToProperty(attNames[i]), listener, sc_IListener_c.VALUE_VALIDATED);
         }
      }
      var domEvents = this.domEvents;
      if (domEvents != null) {
         sc_Bind_c.removeListener(this, null, listener, sc_IListener_c.LISTENER_ADDED);
      }
   }
}

// Called when the DOM element associated with the tag object has changed.
js_HTMLElement_c.domChanged = function(origElem, newElem) {
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
   if (newElem !== null) {
      var curListeners = this.getNeededListeners();
      if (curListeners != null) {
         if (this._eventListeners != null) 
             console.log("*** error: replacing element event listeners");
         for (var i = 0; i < curListeners.length; i++) {
            var listener = curListeners[i];

            if (!listener.alias) {
               if (this[listener.propName] === undefined)
                  this[listener.propName] = null; // set the event property to null initially the first time we have someone listening on it.  this is too late but do we want to initialize all of these fields to null on every tag object just so they are null, not undefined?   Just do not override an existing value or refreshBinding fires when we do not want it to
            }
            else // Now that we know the value of the aliased property (e.g. innerHeight) we need to send a change event cause it changes once we have an element.
               sc_Bind_c.sendChangedEvent(this, listener.propName);

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
      var style = this.style;
      if (style != null && style != newElem.getAttribute("style"))
         newElem.setAttribute("style", this.style); 
   }
}

js_HTMLElement_c.initListener = function(listener, prop, scEventName) {
   if (scEventName == null)
      scEventName = prop;
   else
      listener.alias = true; // This is like innerWidth which is mapped to a separate resizeEvent
   // Convert from the sc event name, e.g. clickEvent to click
   var jsEventName = scEventName.substring(0, scEventName.indexOf("Event")).toLowerCase();
   listener.eventName = jsEventName;
   listener.scEventName = scEventName;
   listener.propName = prop;
   listener.callback = sc_newEventArgListener(js_HTMLElement_c.eventHandler, listener);
}

function js_initDomListener(listener, prop, eventName, res) {
   // For efficiency, each domEvent stores initially an empty object.  We lazily create the string names and callback to avoid creating them for each instance.
   if (listener.callback == null) {
       js_HTMLElement_c.initListener(listener, prop, eventName);
   }
   if (res == null)
      res = [];
   res.push(listener);
   return res;
}

// For the given HTMLElement instance, return the array of listeners for dom events we need to listen on.  This will correspond to the set of clickEvent, etc. properties 
// which have listeners on them.  For those only we need to listen to the underlying DOM event.  If we listened to every DOM event for every tag object it would be 
// expensive so this optimization will help a lot.  The other piece to the puzzle is that we need to listen for new bindings added to this object and add the corresponding
// listener when someone starts listening on an event object.
js_HTMLElement_c.getNeededListeners = function() {
   var listeners = sc_getBindListeners(this);
   var res = null;
   var domEvents = this.domEvents;
   var domAliases = this.domAliases;
   for (var prop in listeners) {
      if (prop != null && listeners.hasOwnProperty(prop)) {
         var plist = listeners[prop];
         if (plist != null) {
            var listener = null;
            var eventName = null;
            var handled = false;
            if (domEvents.hasOwnProperty(prop)) {
               listener = domEvents[prop];
            }
            else if (domAliases.hasOwnProperty(prop)) {
               var eventNameList = domAliases[prop];
               // The alias may require multiple events - e.g. mouseOverEvent and mouseOutEvent for hovered
               if (sc_instanceOf(eventNameList, Array)) {
                  for (var i = 0; i < eventNameList.length; i++) {
                     var eventName = eventNameList[i];
                     listener = domEvents[eventName];
                     res = js_initDomListener(listener, prop, eventName, res);
                  }
                  handled = true;
               }
               else {
                  eventName = eventNameList;
                  listener = domEvents[eventName];
               }
            }
            if (listener != null && !handled) {
               res = js_initDomListener(listener, prop, eventName, res);
            }
         }
      }
   }
   return res;
}

js_HTMLElement_c.setRepeat = function(r) {
   if (r !== null) {
      if (this.repeatListener === undefined) {
         this.repeatListener = new js_RepeatListener(this);
         sc_Bind_c.addListener(this, "repeat", this.repeatListener, sc_IListener_c.VALUE_VALIDATED);
      }
   }
   this.repeat = r;
   if (r !== null && this.repeatTags === undefined) {
      this.repeatTags = null;
      this.invalidateRepeatTags();
   }
   sc_Bind_c.sendChangedEvent(this, "repeat");
}

js_HTMLElement_c.getRepeat = function() {
   return this.repeat;
}

js_HTMLElement_c.setRepeatVar = function(v) {
   this.repeatVar = v;
   sc_Bind_c.sendChangedEvent(this, "repeatVar");
}

js_HTMLElement_c.getRepeatVar = function() {
   return this.repeatVar;
}

js_HTMLElement_c.setRepeatIndex = function(v) {
   this.repeatIndex = v;
   sc_Bind_c.sendChangedEvent(this, "repeatIndex");
}

js_HTMLElement_c.getRepeatIndex = function() {
   return this.repeatIndex;
}

js_HTMLElement_c.setRepeatVarName = function(vn) {
   this.repeatVarName = vn;
}

js_HTMLElement_c.getRepeatVarName = function() {
   return this.repeatVarName;
}

js_HTMLElement_c.repeatTagIndexOf = function(startIx, repeatVal) {
   var sz = this.repeatTags.length;
   for (var i = startIx; i < sz; i++) {
      if (this.repeatTags[i].getRepeatVar === undefined) {
         console.error("*** invalid repeat tag");
         continue;
      }
      var tagRepeatVar = this.repeatTags[i].getRepeatVar();
      if (tagRepeatVar === repeatVal || (tagRepeatVar != null && tagRepeatVar.equals(repeatVal)))
         return i;
   }
   return -1;
}

js_HTMLElement_c.repeatElementIndexOf = function(repeat, startIx, repeatVal) {
   var sz = sc_DynUtil_c.getArrayLength(repeat);
   for (var i = startIx; i < sz; i++) {
      var arrayVal = sc_DynUtil_c.getArrayElement(repeat, i);
      if (arrayVal === repeatVal || (arrayVal != null && arrayVal.equals(repeatVal)))
         return i;
   }
   return -1;
}

js_HTMLElement_c.initSync = function() {
   if (this.repeat !== null) {
      this.syncRepeatTags(false);
   }
}

js_HTMLElement_c.repeatNeedsSync = function() {
   var repeat = this.repeat;
   var repeatTags = this.repeatTags;
   if (this.repeat === null) {
      return repeatTags != null; // allow undefined and null here
   }
   if (repeatTags == null) // undefined or null
      return true;
   var newSz = sc_DynUtil_c.getArrayLength(repeat);
   var oldSz = repeatTags.length;
   if (newSz != oldSz)
      return true;
   for (var i = 0; i < newSz; i++) {
      var arrayVal = sc_DynUtil_c.getArrayElement(repeat, i);
      var oldElem = repeatTags[i];
      var oldArrayVal = oldElem.getRepeatVar();
      if (!sc_DynUtil_c.equalObjects(oldArrayVal, arrayVal))
         return true;
   }
   return false;
}

js_HTMLElement_c.refreshRepeat = function() {
   if (this.syncRepeatTags(true)) {
      this.refreshBody();
   }
}


// This method gets called when the repeat property has changed.  For each value in the repeat array we create a tag object and corresponding DOM element in repeatTags.
// process incrementally updates one from the other, trying to move and preserve the existing tags when possible because that will lead to incremental UI refreshes.
js_HTMLElement_c.syncRepeatTags = function(updateDOM) {
   var needsRefresh = false;
   var repeat = this.repeat;
   //var oldSyncState = sc_SyncManager_c.getSyncState();
   try {
      //sc_SyncManager_c.setSyncState(sc_SyncManager_SyncState_c.Disabled);

      if (repeat === undefined)
         console.error("Undefined value for repeat property on: " + this);

      var sz = repeat === null ? 0 : sc_DynUtil_c.getArrayLength(repeat);
      if (repeat !== null) {
         this.repeatTagsValid = true;
         var repeatTags = this.repeatTags;
         if (repeatTags === null) {
            this.repeatTags = repeatTags = [];
            for (var i = 0; i < sz; i++) {
               var toAddArrayVal = sc_DynUtil_c.getArrayElement(repeat, i);
               if (toAddArrayVal == null) {
                  console.error("Null or undefined value for repeat element: " + i);
                  continue;
               }
               var newElem = this.createRepeatElement(toAddArrayVal, i, null);
               repeatTags.push(newElem);
            }
         }
         // Incrementally update the tags while keeping the DOM synchronized with the object tree
         else {
            // Walking through the current value of the repeat list
            for (var i = 0; i < sz; i++) {
               var arrayVal = sc_DynUtil_c.getArrayElement(repeat, i); // the value at spot i now
               var curIx = this.repeatTagIndexOf(0, arrayVal); // the index this spot used to be in the list
               // If there is an existing node at this spot
               if (i < repeatTags.length) {
                  var oldElem = repeatTags[i];
                  var oldArrayVal = oldElem.getRepeatVar();
                  // The guy in this spot is not our guy.
                  if (oldArrayVal !== arrayVal && (oldArrayVal == null || !oldArrayVal.equals(arrayVal))) {
                     // The current guy is new to the list
                     if (curIx == -1) {
                        // Either replace or insert a row
                        var curNewIx = this.repeatElementIndexOf(repeat, i, oldArrayVal);
                        if (curNewIx == -1) { // Reuse the existing object so this turns into an incremental refresh
                           var newElem = this.createRepeatElement(arrayVal, i, oldElem);
                           if (oldElem == newElem) {
                              oldElem.setRepeatIndex(i);
                              oldElem.setRepeatVar(arrayVal);
                           }
                           else {
                              needsRefresh = this.removeElement(oldElem, i, updateDOM) || needsRefresh;
                              needsRefresh = this.insertElement(newElem, i, updateDOM) || needsRefresh;
                           }
                        }
                        else {
                           // Assert curNewIx > i - if it is less, we should have already moved it when we processed the old guy
                           var newElem = this.createRepeatElement(arrayVal, i, null);
                           needsRefresh = this.insertElement(newElem, i, updateDOM) || needsRefresh;
                        }
                     }
                     // The current guy is in the list but later on
                     else {
                        var elemToMove = repeatTags[curIx];
                        // Try to delete our way to the old guy so this stays incremental.  But at this point we also delete all the way to the old guy so the move is as short as possible (and to batch the removes in case this ever is used with transitions)
                        var delIx;
                        var needsMove = false;
                        for (delIx = i; delIx < curIx; delIx++) {
                           var delElem = repeatTags[i];
                           var delArrayVal = delElem.getRepeatVar();
                           var curNewIx = this.repeatElementIndexOf(repeat, i, delArrayVal);
                           if (curNewIx == -1) {
                              needsRefresh = this.removeElement(delElem, i, updateDOM) || needsRefresh;
                           }
                           else
                              needsMove = true;
                        }
                        // If we deleted up to the current, we are done.  Otherwise, we need to re-order
                        if (needsMove) {
                           elemToMove.setRepeatIndex(i);
                           needsRefresh = this.moveElement(elemToMove, curIx, i, updateDOM) || needsRefresh;
                        }
                     }
                  }
               }
               else {
                  // If the current array val is not in the current list then append it
                  if (curIx == -1) {
                     var arrayElem = this.createRepeatElement(arrayVal, i, null);
                     needsRefresh = this.appendElement(arrayElem, updateDOM) || needsRefresh;
                  }
                  // Otherwise need to move it into its new location.
                  else {
                     var elemToMove = repeatTags[curIx];
                     elemToMove.setRepeatIndex(i);
                     needsRefresh = this.moveElement(elemToMove, curIx, i, updateDOM) || needsRefresh;
                  }
               }
            }

            while (repeatTags.length > sz) {
               var ix = repeatTags.length - 1;
               var toRem = repeatTags[ix];
               needsRefresh = this.removeElement(toRem, ix, updateDOM) || needsRefresh;
               if (needsRefresh) // Unable to remove - need to get out.
                  break;
            }
         }
      }
      else { // TODO: dispose of old tags?
         this.destroyRepeatTags();
         repeatTags = [];
      }
   }
   finally {
      //sc_SyncManager_c.setSyncState(oldSyncState);
   }
   return needsRefresh;
}

js_HTMLElement_c.appendElement = function(tag, updateDOM) {
   if (updateDOM) {
      var sz = this.repeatTags.length;
      // No existing element - can't incrementall update the DOM.  So just update the repeatTags and refresh.
      if (sz == 0) {
         this.repeatTags.push(tag);
         return true;
      }

      var repeatTag = this.repeatTags[sz-1];
      var curElem = repeatTag.element;
      if (curElem == null) {
         repeatTag.updateDOM();
         curElem = repeatTag.element;
         // No previous node in the list - need to record this guy and refresh
         if (curElem == null) {
            this.repeatTags.push(tag);
            return true;
         }
      }

      var tmp = document.createElement('div');
      tmp.innerHTML = tag.output().toString();
      // We want to append this element after curElem but there's only "insert before" in the DOM api
      var nextElem = curElem.nextSibling;
      var parentNode = curElem.parentNode;
      if (nextElem === null)
         parentNode.appendChild(tmp.childNodes[0]);
      else
         parentNode.insertBefore(tmp.childNodes[0], nextElem);
      //document.removeChild(tmp); ?? needs to be removed somehow
   }
   this.repeatTags.push(tag);
   tag.updateDOM();
   return false;
}

js_HTMLElement_c.insertElement = function(tag, ix, updateDOM) {
   if (updateDOM) {
       // Can't do an incremental refresh when there is no current element... to do this incrementally maybe we insert a dummy tag when we remove the last one?
       if (ix >= this.repeatTags.length)
          return true;
       var tmp = document.createElement('div');
       tmp.innerHTML = tag.output().toString();
       var repeatTag = this.repeatTags[ix];
       var curElem = repeatTag.element;
       if (curElem == null) {
          repeatTag.updateDOM();
          curElem = repeatTag.element;
          if (curElem == null) {
             if (js_Element_c.verbose)
                console.log("Unable to insert element to repeat - no element in sibling repeat tag at spot: " + ix);
             return true;
          }
       }
       curElem.parentNode.insertBefore(tmp.childNodes[0], curElem);
       //document.removeChild(tmp); ?? does this need to be removed - it throws an exception?
   }
   this.repeatTags.splice(ix, 0, tag);
   tag.updateDOM();
   return false;
}

js_HTMLElement_c.removeElement = function(tag, ix, updateDOM) {
   if (updateDOM) {
      var repeatTag = this.repeatTags[ix];
      var curElem = repeatTag.element;
      if (curElem == null) {
         repeatTag.updateDOM();
         curElem = repeatTag.element;
         if (curElem == null) {
            console.log("Unable to remove element to repeat - no element for repeat tag at spot: " + ix);
            return true;
         }
      }
      curElem.parentNode.removeChild(curElem);
   }
   this.repeatTags.splice(ix,1);
   // Needs to be done after updateDOM as it setss element = null.
   tag.destroy();
   return false;
}

js_HTMLElement_c.moveElement = function(tag, oldIx, newIx, updateDOM) {
   if (updateDOM) {
      var elemToMove = tag.element;
      if (elemToMove == null) {
         tag.updateDOM();
         elemToMove = tag.element;
         if (curElem == null) {
            console.log("Unable to remove element to repeat - no element for repeat tag ");
            return true;
         }
      }
      // The new spot is at the end of the list
      if (newIx >= this.repeatTags.length) {
         elemToMove.parentNode.appendChild(elemToMove);
      }
      else {
         var newSpot = this.repeatTags[newIx];
         if (newSpot == null)
            console.error("No repeat tag at index: " + newIx + " for mov element");
         else
            elemToMove.parentNode.insertBefore(elemToMove, newSpot.element);
      }
   }
   this.repeatTags.splice(newIx, 0, elemToMove);
   return false;
}

js_HTMLElement_c.getId = function() {
   if (this.id != null)
      return this.id;
   if (this.element === null)
      return null;
   return this.element.id;
}

js_HTMLElement_c.setTextContent = function(txt) {
   this.element.textContent = txt;
}

js_HTMLElement_c.getTextContent = function() {
   return this.element.textContent;
}

js_HTMLElement_c.getOffsetWidth = function() {
   if (this.element)
      return this.element.offsetWidth;
   return 0;
}

js_HTMLElement_c.getOffsetHeight = function() {
   if (this.element)
      return this.element.offsetHeight;
   return 0;
}

js_HTMLElement_c.getOffsetLeft = function() {
   if (this.element)
      return this.element.offsetLeft;
   return 0;
}

js_HTMLElement_c.getOffsetTop = function() {
   if (this.element)
      return this.element.offsetTop;
   return 0;
}

js_HTMLElement_c.getChildrenByIdAndType = function(id, type) {
   var res = this.getChildrenById(id);
   if (res == null)
      return res;

   var newRes = [];
   for (var i = 0; i < res.length; i++) {
      var v = res[i];
      if (sc_instanceOf(v, type))
         newRes.push(v);
   }
   return newRes;
}

js_HTMLElement_c.getChildrenById = function(id) {
   if (!this.getObjChildren)
      return null;
   var children = this.getObjChildren(true);
   if (children == null)
      return null;
   if (id == null)
      return children;
   var res = null;
   for (var i = 0; i < children.length; i++) {
      var child = children[i];
      if (child.getId && child.getId() == id) {
         if (res == null) res = [];
         res.push(child);
      }
   }
   return res;
}

js_HTMLElement_c.refresh = function() {
   if (this.serverContent) {
      if (this.element == null) { // Attach to the dom generated by the server
         this.startValid = this.bodyValid = true;
         this.updateDOM();
         if (this.element == null)
            console.error("No element: " + this.getId() + " in the DOM for refresh of serverContent for tag object");
      }
      return;
   }
   this.refreshBody();
   this.refreshStart();
}

js_HTMLElement_c.makeRoot = function() {
   var id = sc_id(this);
   if (sc$rootTags[id] == null) {
      sc$rootTags[id] = this;
      sc$rootTagsArray.push(this);
   }
}

js_HTMLElement_c.refreshBodyContent = function(sb) {
   return this.outputBody(sb);
}

js_HTMLElement_c.refreshBody = function() {
   var create = false;
   this.updateDOM();
   if (this.element === null) {
      // Go to the top level tag and start the output process up there.  It needs to attach us to the tree
      if (this.outer !== undefined && this.outer.refresh !== null) {
         this.bodyValid = true; // or should we mark this as true when the parent refreshes?
         if (js_Element_c.trace)
            console.log("refreshBody of: " + this.id + " refreshing parent: " + this.outer.id);
         this.outer.refresh();
         return;
      }
      else {
         create = true;
      }
   }
   this.bodyValid = true;

   if (this.outer === undefined) {
      this.makeRoot();
   }

   if (create) {
      if (js_Element_c.trace)
         console.log("refreshBody creating DOM for top level node: " + this.id);
      sb = this.output();
      this.bodyValid = true;

      // Top level object - need to replace the body?  Or should we append to it?
      var outRes = sb.toString();
      // When we have an empty document, do not write or it overwrites the previous document, like when we have an empty template and a full one
      if (outRes.length > 16 || outRes.trim().length > 0) {
         if (sc$rootTagsArray.length > 1) {
            var tmp = document.createElement('div');
            tmp.innerHTML = outRes;
            try {
               document.appendChild(tmp.childNodes[0]);
            }
            catch (e) {
               console.error("Error appending child for refreshBody of element: " + this.id);
            }
         }
         else {
            document.write(outRes);
         }
         this.updateDOM();
      }
      if (this.tagName != null && this.element === null)
         console.log("unable to find top-level element after refresh");
   }
   else {
      var sb = new jv_StringBuilder();
      this.refreshBodyContent(sb);
      var newBody = sb.toString();
      try {
         this.element.innerHTML = newBody;
      }
      catch (e) {
         console.error("Failed to update innerHTML due to browser limitation for: " + this.tagName + " id=" + this.getId() + ": " + e);
      }

      if (js_Element_c.trace)
         console.log("refreshBody: " + this.id + (js_Element_c.verbose ? " new content: " + newBody : ""));

      this.updateChildDOMs();
   }
}

js_HTMLElement_c.refreshStart = function() {
    if (this.startValid)  
     return;

   this.startValid = true;
   if (!this.visible)
      this.removeFromDOM();
   // TODO: replacing a tag and it's attributes from HTML is hard!  there's replaceChild but no way to append an HTML string.  Here we want to replace a specific element.  Could call createElement but would need to manually add the attributes.  Could add the element to the parent, find it, then replace it maybe?}
}

js_HTMLElement_c.updateChildDOMs = function() {
   var children = this.getObjChildren ? this.getObjChildren(true) : null;
   if (children !== null) {
      for (var i=0; i < children.length; i++) {
         var child = children[i];
         if (child.updateDOM !== undefined) {
            child.updateDOM();
         }
      }
   }
}

js_HTMLElement_c.output = function() {
   var sb = new jv_StringBuilder();
   this.outputTag(sb);
   return sb;
}

js_HTMLElement_c.createRepeatElement = function(rv, ix, oldTag) {
   var elem;
   var flush = sc_SyncManager_c.beginSyncQueue();
   if (sc_instanceOf(this, js_IRepeatWrapper)) {
       elem = this.createElement(rv, ix, oldTag);
   }
   else {  // TODO: remove? This is the older case where the generated code did not generate the separate wrapper class
      elem = sc_DynUtil_c.createInnerInstance(this.constructor, null, this.outer);
      elem.setRepeatIndex(ix);
      elem.setRepeatVar(rv);

      // TODO: a cleaner solution would be to create separate classes - one for the container, the other for the element so this binding does not get applied, then removed
      sc_Bind_c.removePropertyBindings(elem, "repeat", true, true);
      elem.repeat = null;
   }
   if (elem != null)
      js_HTMLElement_c.registerSyncInstAndChildren(elem);
   if (flush)
       sc_SyncManager_c.flushSyncQueue();
   return elem;
}

js_HTMLElement_c.registerSyncInstAndChildren = function(obj) {
   sc_SyncManager_c.registerSyncInst(obj);
   var children = sc_DynUtil_c.getObjChildren(obj, null, true);
   if (children != null) {
      for (var i = 0; i < children.length; i++) {
         js_HTMLElement_c.registerSyncInstAndChildren(children[i]);
      }
   }
}


js_HTMLElement_c.outputStartTag = function(sb) {
   if (this.tagName != null) {
      sb.append("<");
      sb.append(this.tagName);

      // This is necessary for serverContent objects where you refresh the parent.  Since you
      // are rebuilding the HTML for the parent entirely, we need to extract the static content
      // from the DOM which is not easy for the attributes.
      var elem = this.element;
      if (elem != null) {
         var atts = elem.attributes;
         var len = atts.length;
         for (var i = 0; i < len; i++) {
            var att = atts[i];
            sb.append(" ");
            sb.append(att.name);
            sb.append("=\"");
            sb.append(att.value);
            sb.append('"');
         }
      }
      sb.append(">");
   }
}

js_HTMLElement_c.outputEndTag = function(sb) {
   if (this.tagName != null) {
      sb.append("</");
      sb.append(this.tagName);
      sb.append(">");
   }
}

js_HTMLElement_c.outputBody = function(sb) {
}

js_HTMLElement_c.serverContent = false;

js_HTMLElement_c.outputTag = function(sb) {
   if (!this.visible)
      return;
   if (this.repeat !== null) {
      this.syncRepeatTags(true); 
      if (this.repeatTags !== null) {
         for (var i = 0; i < this.repeatTags.length; i++) {
            var rtag = this.repeatTags[i];
            rtag.outputTag(sb);
            rtag.startValid = rtag.bodyValid = true;
         }
      }
   }
   else {
      this.outputStartTag(sb);
      if (this.serverContent) {
         if (this.element != null)
            sb.append(this.element.innerHTML);
         else
            console.error("Missing HTML for server tag object: " + this.getId() + " use style='display:none' instead of visible=false on the initial page load.");
      }
      else
         this.outputBody(sb);
      this.outputEndTag(sb);
   }
}

js_HTMLElement_c.invalidate = function() {
   this.invalidateStartTag();
   this.invalidateBody();
}

js_HTMLElement_c.schedRefresh = function(tagList, validName) {
   if (this[validName]) {
      if (!js_Element_c.globalRefreshScheduled) {
         this[validName] = false; 
         if (!js_Element_c.refreshScheduled) {
            js_Element_c.refreshScheduled = true;
            sc_addRunLaterMethod(this, js_Element_c.refreshTags, 5);
         }
         tagList.push(this);
      }
   }
}

js_HTMLElement_c.invalidateBody = function() {
   this.schedRefresh(js_Element_c.refreshBodyTags, "bodyValid");
}

js_HTMLElement_c.invalidateStartTag = function() {
   this.schedRefresh(js_Element_c.refreshStartTags, "startValid");
}

js_HTMLElement_c.invalidateRepeatTags = function() {
   var rts = this.repeatTags;
   var needsRefresh = rts == null;
   if (!needsRefresh) {
      for (var i = 0; i < rts.length; i++) {
         if (rts[i].element == null) {
            needsRefresh = true;
            break;
         }
      }
   }
   if (needsRefresh)
      this.invalidateBody();
   else
      this.schedRefresh(js_Element_c.refreshRepeatTags, "repeatTagsValid");
}

js_HTMLElement_c.domEvents = {clickEvent:{}, dblClickEvent:{}, mouseDownEvent:{}, mouseMoveEvent:{}, mouseOverEvent:{aliases:["hovered"], computed:true}, mouseOutEvent:{aliases:["hovered"], computed:true}, mouseUpEvent:{}, keyDownEvent:{}, keyPressEvent:{}, keyUpEvent:{}, submitEvent:{}, changeEvent:{}, blurEvent:{}, focusEvent:{}, resizeEvent:{aliases:["innerWidth","innerHeight"]}};
js_HTMLElement_c.domAliases = {innerWidth:"resizeEvent", innerHeight:"resizeEvent", hovered:["mouseOverEvent","mouseOutEvent"]};

// Initialize the domEvent properties as null at the class level so we do not have to maintain them for each tag instance.
var domEvents = js_HTMLElement_c.domEvents;
for (var prop in domEvents) {
   if (domEvents.hasOwnProperty(prop))
      js_HTMLElement_c[prop] = null;
}

// For IE we need to map the srcElement to the element who is listening for this property
function js_findCurrentTarget(elem, prop) {
   do {
      if (elem.scObj != null && sc_PBindUtil_c.getBindingListeners(elem.scObj, prop) != null)
         return elem;
      elem = elem.parentNode;
   } while (elem != null);

   return null;
}

// Just like the above but only need to verify that we have the property 
function js_findCurrentTargetSimple(elem, prop) {
   do {
      if (elem.scObj != null && sc_hasProp(elem.scObj,prop))
         return elem;
      elem = elem.parentNode;
   } while (elem != null);

   return null;
}

js_HTMLElement_c.eventHandler = function(event, listener) {
   var elem = event.currentTarget ? event.currentTarget : js_findCurrentTarget(event.srcElement, listener.propName);
   js_HTMLElement_c.processEvent(elem, event, listener);
}

js_HTMLElement_c.processEvent = function(elem, event, listener) {
   var scObj = elem.scObj;
   if (scObj !== undefined) {

      // Add this as a separate field so we can use the exposed parts of the DOM api from Java consistently
      event.currentTag = scObj;
      var eventValue;

      // e.g. innerWidth or hovered
      if (listener.alias != null) {
         // e.g. hovered which depends on mouseOut/In - just fire the change event as the property is changed in the DOM api
         var computed = listener.computed;
         var origValue = null;  
         if (computed) {
            origValue = sc_DynUtil_c.getPropertyValue(scObj, listener.propName);
            // The getX method (e.g. getHovered) needs the info from the event to compute it's value properly
            scObj[listener.scEventName] = eventValue;
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
         console.log("tag event: " + listener.propName + ": " + listener.scEventName + " = " + eventValue);
      sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, scObj, listener.propName, eventValue);

      // TODO: for event properties should we delete the property here or set it to null?  flush the queue of events if somehow a queue is enabled here?
   }
   else 
      console.log("Unable to find scObject to update in eventHandler");
}

js_HTMLElement_c.getInnerWidth = function() {
   if (this.element == null)
      return 0;
   return this.element.clientWidth;
}

js_HTMLElement_c.getInnerHeight = function() {
   if (this.element == null)
      return 0;
   return this.element.clientHeight;
}

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
         console.log("Setting hovered: " + isMouseOver + " on: " + this.getId());
         this.hovered = isMouseOver;
      }
      else
         console.log("Skipping hovered for: " + isMouseOver + " on: " + this.getId());
   }
   if (this.hovered !== undefined)
      return this.hovered;
   return false;
}

function js_Input() {
   js_HTMLElement.call(this);
   this.value = null;
   this.type = "text";
   this.clickCount = 0;
   this.checked = false;
}
js_Input_c = sc_newClass("js_Input", js_Input, js_HTMLElement, null);

js_Input_c.refreshAttNames = js_HTMLElement_c.refreshAttNames.concat(["value", "disabled", "checked"]);
js_Input_c.removeOnEmpty = {value:true};

//js_Input_c.booleanAtts = {checked:true};

js_Input_c.doChangeEvent = function(event) {
   var elem = event.currentTarget ? event.currentTarget : event.srcElement;
   var scObj = elem.scObj;
   if (scObj !== undefined) {
      if (scObj.setValue) {
         scObj.setValue(this.value);
         if (this.value == "" && scObj.removeOnEmpty.value != null)
            this.removeAttribute("value");
      }
      if (scObj.setChecked)
         scObj.setChecked(this.checked);

      sc_refresh();
   }
   else 
      console.log("Unable to find scObject to update in doChangeEvent");
}

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
      if (this.value != null)
         newElem.value = this.value; 
   }
}

js_Input_c.setValue = function(newVal) {
   if (newVal != this.value) {
      this.value = newVal; 
      if (this.element !== null && this.element.value != newVal)
         this.element.value = newVal;
      sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "value" , newVal);
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
      sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "disabled" , newVal);
   }
}

js_Input_c.getDisabled = function() {
   return this.disabled; 
}

js_Input_c.setClickCount = function(ct) {
   if (ct != this.clickCount) {
      this.clickCount = ct; 
      sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "clickCount" , this.clickCount);
   }
}

js_Input_c.getClickCount = function() {
   return this.clickCount; 
}

js_Input_c.setChecked = function(ct) {
   if (ct != this.checked) {
      this.checked = ct; 
      sc_Bind_c.sendChangedEvent(this, "checked");
   }
}

js_Input_c.getChecked = function() {
   return this.checked; 
}

function js_Button() {
   js_Input.call(this);
}

js_Button_c = sc_newClass("js_Button", js_Button, js_Input, null);

js_Button_c.domChanged = function(origElem, newElem) {
   js_HTMLElement_c.domChanged.call(this, origElem, newElem);

   if (origElem != null) {
      sc_removeEventListener(origElem, 'click', js_Button_c.doClickCount);
   }
   if (newElem != null) {
      sc_addEventListener(newElem, 'click', js_Button_c.doClickCount);
   }
}

js_Button_c.doClickCount = function(event) {
   var elem = event.currentTarget ? event.currentTarget : js_findCurrentTargetSimple(event.srcElement, "clickCount");
   if (elem.scObj !== undefined)
      elem.scObj.setClickCount(elem.scObj.getClickCount() + 1);
   else 
      console.log("Unable to find scObject to update in doClickCount");
}

function js_A() {
   js_HTMLElement.call(this);
   this.tagName = "a";
   this.clickCount = 0;
}

js_A_c = sc_newClass("js_A", js_A, js_HTMLElement, null);
js_A_c.domChanged = js_Button_c.domChanged;
js_A_c.doClickCount = js_Button_c.doClickCount;
js_A_c.setClickCount = js_Input_c.setClickCount;
js_A_c.getClickCount = js_Input_c.getClickCount;

function js_SelectListener(scObj) {
   sc_AbstractListener.call(this);
   this.scObj = scObj;
}

js_SelectListener_c = sc_newClass("js_SelectListener", js_SelectListener, sc_AbstractListener, null);

js_SelectListener_c.valueValidated = function(obj, prop, detail, apply) {
   var scObj = this.scObj;
   scObj.invalidate();
}

function js_Select() {
   js_HTMLElement.call(this);
   this.tagName = "select";
   this.optionDataSource = null;
   this.selectedIndex = -1;
   this.selectedValue = null;
}
js_Select_c = sc_newClass("js_Select", js_Select, js_HTMLElement, null);

js_Select_c.doChangeEvent = function(event) {
   var elem = event.currentTarget ? event.currentTarget : js_findCurrentTargetSimple(event.srcElement, "selectedIndex");
   var scObj = elem.scObj;
   if (scObj !== undefined) {
      var ix = elem.selectedIndex;
      scObj.setSelectedIndex(ix);
   }
   else 
      console.log("Unable to find scObject to update in doChangeEvent");
}

js_Select_c.domChanged = function(origElem, newElem) {
   js_HTMLElement_c.domChanged.call(this, origElem, newElem);
   if (origElem != null)
      sc_removeEventListener(origElem, 'change', js_Select_c.doChangeEvent);
   if (newElem != null) {
      sc_addEventListener(newElem, 'change', js_Select_c.doChangeEvent);
      newElem.selectedIndex = this.selectedIndex;
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
      sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "selectedIndex" , newIx);
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
      sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "selectedValue" , newVal);
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
   sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "optionDataSource" , newDS);
}

js_Select_c.getOptionDataSource = function() {
   return this.optionDataSource; 
}

// TODO: the select tag should probably use the "repeat" property which it already inherits.  For now it's an experiment in 
// a different way to implement sub-tags which is not stateful, more code-control by rendering each option after setting the data source.  
// The outputBody method is not really used here directly which means we have to override refreshBody as well.
js_Select_c.outputTag = function(sb) {
   var ds = this.optionDataSource;
   if (ds == null) {
      js_HTMLElement_c.outputTag.call(this, sb);
      return;
   }
   this.outputStartTag(sb);
   this.outputSelectBody(sb);
   this.outputEndTag(sb);
}

js_Select_c.outputSelectBody = function(sb) {
   var ds = this.optionDataSource;
   var dataLen = ds == null ? 0 : sc_length(ds);
   if (dataLen == 0) {
      var emptyIds = this.getChildrenById("empty");
      if (emptyIds != null) {
         for (var i = 0; i < emptyIds.length; i++) {
             emptyIds.outputTag(sb);
         }
      }
      return;
   }

   var defChildren = this.getChildrenByIdAndType(null, js_Option);

   for (var dix = 0; dix < dataLen; dix++) {
      var dv = sc_arrayValue(ds, dix);
      var selected = dix == this.selectedIndex;
      if (defChildren == null) {
         sb.append("<option");
         if (selected)
            sb.append(" selected");
         sb.append(">");
         sb.append(dv);
         sb.append("</option>");
      }
      else {
         var subOption = defChildren[dix % defChildren.length];
         // disable refresh by marking this invalid before we change the data
         subOption.bodyValid = false;
         subOption.startValid = false;
         subOption.setSelected(selected);
         subOption.setOptionData(dv);
         subOption.outputTag(sb);
         subOption.bodyValid = true;
         subOption.startValid = true;
      }
   }
}

js_Select_c.refreshBodyContent = function(sb) {
   this.outputSelectBody(sb);
}

function js_Option() {
   js_HTMLElement.call(this);
   this.tagName = "option";
   this.optionData = null;
   this.selected = false;

}
js_Option_c = sc_newClass("js_Option", js_Option, js_HTMLElement, null);

js_Option_c.refreshAttNames = js_HTMLElement_c.refreshAttNames.concat["selected", "disabled", "value"];

js_Option_c.setOptionData = function(dv) {
   this.optionData = dv;
   sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "optionData" , dv);
}
js_Option_c.getOptionData = function() {
   return this.optionData;
}

js_Option_c.setSelected = function(newSel) {
   if (newSel != this.selected) {
      this.selected = newSel; 
      if (this.element !== null && this.element.selected != newSel)
         this.element.selected = newSel;
      sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "selected" , newSel);
   }
}

js_Option_c.getSelected = function() {
   return this.selected; 
}

function js_Form() {
   js_HTMLElement.call(this);
   this.tagName = "form";
   this.submitCount = 0;
}
js_Form_c = sc_newClass("js_Form", js_Form, js_HTMLElement, null);

js_Form_c.submitEvent = function(event) {
   var elem = event.currentTarget ? event.currentTarget : js_findCurrentTargetSimple(event.srcElement, "submitCount");
   var scObj = elem.scObj;
   if (scObj !== undefined)
      scObj.setSubmitCount(scObj.getSubmitCount()+1);
   else 
      console.log("Unable to find scObject to update in submitEvent");
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
      sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "submitCount" , this.submitCount);
   }
}

js_Form_c.getSubmitCount = function() {
   return this.submitCount; 
}

function js_Page() {
   js_HTMLElement.call(this);
   // Schedule a refresh once this script is done loading
   sc_addLoadMethodListener(this, js_Page_c.refresh);
   sc_runLaterScheduled = true;
   // Signal to others not to bother refreshing individually - avoids refreshing individual tags when we are going to do it at the page level anyway
   js_Element_c.globalRefreshScheduled = true;
   this.makeRoot();
}

js_Page_c = sc_newClass("js_Page", js_Page, js_HTMLElement, null);

js_Page_c.refresh = function() {
   // Do this right before we refresh.  That delays them till after the script code in the page 
   // has been loaded so all of the dependencies are satisfied.  
   sc_runRunLaterMethods();
   // TODO: should we be refreshing only the body tag, not the head and HTML?
   /*
   if (this.body != null)
      this.body.refresh();
   else */
   js_HTMLElement_c.refresh.call(this);
   js_Element_c.globalRefreshScheduled = false;
}

function js_Div() {
   js_HTMLElement.call(this);
   this.tagName = "div";
}
js_Div_c = sc_newClass("js_Div", js_Div, js_HTMLElement, null);

function js_Span() {
   js_HTMLElement.call(this);
   this.tagName = "span";
}
js_Span_c = sc_newClass("js_Span", js_Span, js_HTMLElement, null);

function js_Head() {
   js_HTMLElement.call(this);
   this.tagName = "head";
}
js_Head_c = sc_newClass("js_Head", js_Head, js_HTMLElement, null);

function js_Body() {
   js_HTMLElement.call(this);
   this.tagName = "body";
}
js_Body_c = sc_newClass("js_Body", js_Body, js_HTMLElement, null);

function js_Html() {
   js_Page.call(this);
   this.tagName = "html";
}
js_Html_c = sc_newClass("js_Html", js_Html, js_HTMLElement, null);

js_Page_c._updateInst = js_Html_c._updateInst = function() {
   // TODO: should we invalidate the body here?
}

function js_HtmlPage() {
   js_Html.call(this);
}

js_HtmlPage_c = sc_newClass("js_HtmlPage", js_HtmlPage, js_Html, null);

function js_Img() {
   js_HTMLElement.call(this);
   this.tagName = "img";
   this.src = null;
   this.height = this.width = 0;
}

js_Img_c = sc_newClass("js_Img", js_Img, js_HTMLElement, null);
js_Img_c.refreshAttNames = js_HTMLElement_c.refreshAttNames.concat(["src", "width", "height"]);

js_Img_c.setSrc = function(newSrc) {
   if (newSrc != this.src) {
      this.src = newSrc; 
      if (this.element !== null && this.element.src != newSrc) {
         this.element.src = newSrc == null ? "" : newSrc;
      }
      sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "src" , newSrc);
   }
}

js_Img_c.getSrc = function() {
   return this.src; 
}

function js_RefreshAttributeListener(scObj) {
   sc_AbstractListener.call(this);
   this.scObj = scObj;
}

function js_Style() {
   js_HTMLElement.call(this);
   this.tagName = "style";
}

js_Style_c = sc_newClass("js_Style", js_Style, js_HTMLElement, null);

function js_StyleSheet() {
   js_Style.call(this);
   this.elementCreated = false;
   this.id = sc_CTypeUtil_c.getClassName(sc_DynUtil_c.getTypeName(this,false)) + ".css";
   this.tagName = null;
   // Schedule a refresh once this script is done loading
   sc_addLoadMethodListener(this, js_StyleSheet_c.refresh);
}

js_StyleSheet_c = sc_newClass("js_StyleSheet", js_StyleSheet, js_Style, null);

js_StyleSheet_c.initDOM = function() {
   if (!this.elementCreated) {
      this.elementCreated = true;
      // TODO: should be disabling the previous style sheet - find it in the head via it's id and set "disabled=true".  For now, we'll rely on the fact that
      // most style sheets will just replace styles with the new values.

      var heads = document.getElementsByTagName("head");
      if (heads == null) {
         console.log("Error no head tag to insert dynamic style sheet content");
         return;
      }
      var style = document.createElement("style");
      style.id = this.getId(); // So we can find this style again if the parent has to refresh
      style.type = "text/css";
      // TODO: for IE, this may be the only way to get it to update the style sheet on the fly.  I think this would go in refreshBody where we do the appendChild on the tag.
      // style.styleSheet.cssText = output().toString()
      heads[0].appendChild(style);
      this.element = style;
      style.scObj = this;
   }
}

js_StyleSheet_c.invalidate = function() {
   this.initDOM();
   js_Style_c.invalidate.call(this);
}

js_StyleSheet_c.refresh = function() {
   this.initDOM();
   js_HTMLElement_c.refresh.call(this);
}

js_StyleSheet_c.refreshBody = function() {
   this.initDOM();
   js_Style_c.refreshBody.call(this);
   if (js_Element_c.verbose)
      console.log("refreshed stylesheet " + this.id + " with:\n" + this.element.innerHTML);
}

/*
js_StyleSheet_c.outputEndTag = js_StyleSheet_c.outputStartTag = function() {
}
*/

js_StyleSheet_c.updateDOM = function() {
   js_HTMLElement_c.updateDOM.call(this);
}

js_StyleSheet_c.setDOMElement = function(elem) {
   js_HTMLElement_c.setDOMElement.call(this, elem);
}

js_RefreshAttributeListener_c = sc_newClass("js_RefreshAttributeListener", js_RefreshAttributeListener, sc_AbstractListener, null);

js_RefreshAttributeListener_c.valueValidated = function(obj, prop, detail, apply) {
   if (this.scObj.element !== null) {
      var newVal = obj[prop];
      var att = js_HTMLElement_c.mapPropertyToAttribute(prop);
      if (newVal !== this.scObj.element[att]) {
         if (newVal == null || (this.scObj.removeOnEmpty[att] !== null && newVal == ''))
            this.scObj.element.removeAttribute(att);
         else
            this.scObj.element[att] = newVal;
      }
   }
}

js_RefreshAttributeListener_c.initAddListener = function(elem, domListener, prop, aliasName) {
   // For efficiency, each domEvent stores initially an empty object.  We lazily create the string names and callback to avoid creating them for each instance.
   if (domListener.callback == null) {
      js_HTMLElement_c.initListener(domListener, prop, aliasName);
   }
   sc_addEventListener(elem, domListener.eventName, domListener.callback);
   if (this._eventListeners == null)
      this._eventListeners = [domListener];
   else
      this._eventListeners.push(domListener);
}

// Gets called from the data binding system when a new binding is added to obj for prop.  If we already have a DOM element
// associated with this tag object, 
js_RefreshAttributeListener_c.listenerAdded = function(obj, prop, newListener, eventMask, priority) {
   var tagObj = this.scObj;
   if (tagObj != null) {
      var elem = tagObj.element;
      if (elem != null) {
         var domListener = tagObj.domEvents[prop];
         var aliasName = null;
         var handled = false;
         if (domListener == null) {
            var aliasNameList = tagObj.domAliases[prop];
            if (sc_instanceOf(aliasNameList, Array)) {
               for (var i = 0; i < aliasNameList.length; i++) {
                  aliasName = aliasNameList[i];
                  domListener = tagObj.domEvents[aliasName];
                  this.initAddListener(elem, domListener, prop, aliasName);
               }
               handled = true;
            }
            else if (aliasNameList != null) {
               aliasName = aliasNameList;
               domListener = tagObj.domEvents[aliasName];
            }
         }
         if (domListener != null && !handled) {
            this.initAddListener(elem, domListener, prop, aliasName);
         }
      }
   }
}

js_RefreshAttributeListener_c.toString = function() {
   return "<" + this.scObj.tagName + " id=" + this.scObj.id + " (attributes)>";
}

function js_RepeatListener(scObj) {
   sc_AbstractListener.call(this);
   this.scObj = scObj;
}

js_RepeatListener_c = sc_newClass("js_RepeatListener", js_RepeatListener, sc_AbstractListener, null);

js_RepeatListener_c.valueValidated = function(obj, prop, detail, apply) {
   var scObj = this.scObj;
   // When an update occurs to the repeat element, check if we need to refresh the list
   if (scObj.repeatTags === null || scObj.repeatNeedsSync()) {
      scObj.invalidateRepeatTags();
   }
}

function js_IRepeatWrapper() {}

js_IRepeatWrapper_c = sc_newClass("js_IRepeatWrapper", js_IRepeatWrapper, null, null);

function js_Document(wrapped) {
   js_HTMLElement.call(this);
   this.setDOMElement(wrapped);
}

js_Document_c = sc_newClass("js_Document", js_Document, js_HTMLElement, null);

function errorCountChanged() {
   sc_Bind_c.sendChangedEvent(js_Window_c.getWindow(), "errorCount");
}

function js_Window() {
   js_Window_c.windowWrapper = this; // avoid recursive infinite loop
   this.document = document;
   this.location = window.location;
   this.documentTag = new js_Document(document);
   window.sc_errorCountListener = errorCountChanged;
}

js_Window_c = sc_newClass("js_Window", js_Window, null, null);

js_Window_c.getWindow = function() {
   if (js_Window_c.windowWrapper === undefined) {
      js_Window_c.windowWrapper = new js_Window();
      if (window.sc_errorCount != undefined)
         errorCountChanged();
      else
         window.sc_errorCount = 0;
   }
   return js_Window_c.windowWrapper;
}

js_Window_c.getInnerWidth = function() {
   js_Window_c.initResizeEvent();
   return window.innerWidth;   
}

js_Window_c.getInnerHeight = function() {
   js_Window_c.initResizeEvent();
   return window.innerHeight;   
}

js_Window_c.getErrorCount = function() {
   if (window.sc_errorCount === undefined)
      window.sc_errorCount = 0;
   return window.sc_errorCount;
}

js_Window_c.initResizeEvent = function() {
   if (js_Window_c._scInitResize === undefined) {
      sc_addEventListener(window, "resize", function(event) {
          var win = js_Window_c.getWindow();
          sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, win, "innerWidth" , win.getInnerWidth());
          sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, win, "innerHeight" , win.getInnerHeight());
      });
      js_Window_c._scInitResize = true;
   }
}


var _c = js_Element_c._dynChildManager = new Object();

_c.addChild = function() {
  // as part of the code changes will update the getObjChildren method which defines the list so
  // nothing to do here.   updating the type schedules the refresh.
}

_c.removeChild = function(par,child) {
}

_c.getObjChildren = function(par) {
   if (par.getObjChildren != null)
      return par.getObjChildren();
   return null;
}

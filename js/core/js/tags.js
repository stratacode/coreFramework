// You can use this type directly and get the basic functionality for any HTML tag but typically it is
// used by generated schtml templates.
//
// js_HTMLElement is the base type for the js class which attaches to and controls the DOM element.  
// It implements change functionality, events bindings, repeat, refresh, etc.
// Subclasses include js_Page, js_Html, js_Body, etc. which can add data binding or other behavior for
// specific attributes.  
function js_HTMLElement() {
   this.refreshScheduled = false;
   this.bodyValid = true;
   this.startValid = true;
   this.repeatTagsValid = true;
   this.tagName = null;
   this.element = null;
   this.id = null;
   this.repeat = null;
   this.replaceWith = null;
   if (arguments.length === 3) {
      this.parentNode = arguments[0];
      this.repeatVar = arguments[1];
      this.repeatIndex = arguments[2];
   }
   else if (arguments.length === 0) {
      this.repeatVar = null;
      this.repeatIndex = -1;
   }
   else
      console.error("Unrecognized constructor for tag object");
   this.repeatVarName = null;
   this.repeatListener = null;
   this.serverRepeat = false;
   this.HTMLClass = null;
   this.visible = true;
   this.parentNode = null;
   this.invisTags = null;
   this.initState = 0;
   this.changedCount = 0;
   this.initScript = null;
   this.stopScript = null;

   // If this is a repeat wrapper, add a listener for the 'repeat' property change
   if (this.isRepeatTag())
      this.initRepeatListener();
}

js_indexPattern = "/index.html";
js_Element_c = js_HTMLElement_c = sc_newClass("sc.lang.html.HTMLElement", js_HTMLElement, jv_Object, [sc_IChildInit, sc_IStoppable, sc_INamedChildren, sc_IObjChildren]);

// This is part of the SemanticNode class on the server and so the component code gen uses it even for client code
// involving component types which extend Element.  Just noop it here.
js_Element_c.init = js_Element_c.start = function() {};
js_Element_c.stop = function() {
    this.visible = false;
}
js_Element_c.tagsToRefresh = [];
js_Element_c.anyRefreshScheduled = false;
js_Element_c.globalRefreshScheduled = false;
js_Element_c.trace = false;
js_Element_c.verbose = false;
js_Element_c.verboseRepeat = false;
js_Element_c.refreshBindings = false;
js_Element_c.refreshOnLoad = true;
js_Element_c.wrap = js_Element_c.bodyOnly = false;
js_Element_c.pendingType = js_Element_c.pendingEvent = null;
js_Element_c.isPageElement = function() { return false; }

var sc_resizeObserver = null;

/*
js_Element_c.getURLPaths = function() {
   return [];
}
*/
js_Element_c.getName = function() { return this.$protoName; }

js_Element_c.doRefreshTags = function(tagList) {
   for (var i = 0; i < tagList.length; i++) {
      var tag = tagList[i];
      tag.refreshScheduled = false;
      var encl = tag.getEnclosingTag();
      if (encl != null && !encl.bodyValid) {
         if (js_Element_c.trace)
            console.log("skipping: " + refName + " for: " + tag.getId() + " waiting for parent to refresh: " + encl.getId());
         continue;
      }
      if (!tag.repeatTagsValid) {
         tag.refreshRepeat();
      }
      if (!tag.startValid) {
         if (!tag.bodyValid)
            tag.refresh();
         else
            tag.refreshStart();
      }
      else if (!tag.bodyValid) {
         tag.refreshBody();
      }
   }
}

// Called when the type is modified 
js_Element_c._updateInst = function() {
   this.invalidate();
}

js_Element_c.refreshCount = 0;
js_Element_c.refreshTags = function() {
   try {
      sc_checkRefresh();

      var toRefresh = js_Element_c.tagsToRefresh;
      js_Element_c.tagsToRefresh = [];
      // TODO: could optimize this by sorting or removing child nodes whose parents are in the refresh list.  If we do refresh a higher level item before a lower level one, the child validate call at least should not happpen since we validate it already.
      js_Element_c.doRefreshTags(toRefresh);

      if (js_Element_c.tagsToRefresh.length > 0) {
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
      js_Element_c.anyRefreshScheduled = false;
   }
}

js_Element_c.getObjChildren = function(create) {
   if (this.replaceWith != null)
      return this.replaceWith.getObjChildren(create);
   if (this.repeatTags != null)
      return this.repeatTags;
   return null;
}

js_Element_c.getNameForChild = function(obj) {
    if (this.repeatTags == null)
       return null;
    for (var i = 0; i < this.repeatTags.length; i++) {
       if (this.repeatTags[i] === obj)
          return sc_CTypeUtil_c.getClassName(sc_DynUtil_c.getTypeName(obj, false)) + "_" + i;
    }
    return null;
}

js_Element_c.getChildForName = function(name) {
   if (!this.isRepeatTag())
       return null;

   // If we are looking up a child name as part of a sync layer, or other situations where a sync queue is involved,
   // when this tag has received a change event, we the repeat value is possibly invalid and depends on an event in
   // the queue. So when we flush these events, the setRepeat method is called or otherwise our value is updated so
   // we can sync the list, then return the list item.
   if (!this.repeatTagsValid || !this.bodyValid) {
      var sync = typeof sc_SyncManager_c != "undefined";
      var bindCtx = sc_BindingContext_c.getBindingContext();
      if (sync && bindCtx != null) {
         sc_SyncManager_c.flushSyncQueue();
         bindCtx.dispatchEvents(null);
      }
      this.refreshRepeat(false);
   }

   if (this.repeatTags == null)
      return null;

   var uix = name.lastIndexOf('_');
   if (uix === -1)
      return null;
   var uixVal = name.substring(uix+1);
   if (uixVal.length === 0)
      return null;
   var ix = parseInt(uixVal);
   if (Number.isNaN(ix) || ix < 0 || ix >= this.repeatTags.length)
      return null;
   return this.repeatTags[ix];
}

js_Element_c.escAtt = function(input, s) {
   if (input == null)
      return "";
   return !s ? input.toString().replace(/"/g, "&quot;") : input.toString().replace(/'/g, "&#039;");
}

js_Element_c.escBody = function(input) {
   if (input == null)
      return "";
    return input.toString()
         .replace(/&/g, "&amp;")
         .replace(/</g, "&lt;")
         .replace(/>/g, "&gt;")
         .replace(/"/g, "&quot;")
         .replace(/'/g, "&#039;");
}

js_Element_c.getRelURL = function(srcRelPath, urlPath) {
   var pref = js_Element_c.getRelPrefix(srcRelPath);
   return pref + (pref.endsWith("/") ? "" : "/") + urlPath;
}

js_Element_c.getRelFileList = function(fileList) {
   return fileList;
}

// JS version of Element.getRelPrefix on server.  Basically, need to convert relative URLs in code on the
// client based on the current URL path name.  This returns the prefix to append into a URL that's reference
// comes from srcRelPath.
js_Element_c.getRelPrefix = function(srcRelPath) {
   if (srcRelPath == null)
      srcRelPath = "";
   var curRelPath = window.location.pathname;
   if (curRelPath.endsWith("/") && curRelPath.length > 1)
       curRelPath = curRelPath.substring(0, curRelPath.length - 1);
   var ix = curRelPath.lastIndexOf('/');
   if (ix == -1)
      return srcRelPath;
   curRelPath = curRelPath.substring(0, ix);
   if (curRelPath == srcRelPath)
      return srcRelPath;

   var curRelDirs = curRelPath.split("/");
   var srcRelDirs = srcRelPath.length == 0 ? [] : srcRelPath.split("/");

   var matchIx = -1;
   var matchLen = Math.min(curRelDirs.length, srcRelDirs.length);
   for (var i = 0; i < matchLen; i++) {
      if (curRelDirs[i] == srcRelDirs[i])
         matchIx = i;
      else
         break;
   }
   var relPath = new jv_StringBuilder();
   for (var i = curRelDirs.length - 1; i > matchIx; i--) {
      relPath.append("../");
   }
   for (var i = matchIx + 1; i < srcRelDirs.length; i++) {
      var srcRelDir = srcRelDirs[i];
      if (srcRelDir.length > 0) {
         relPath.append(srcRelDir);
         relPath.append("/");
      }
   }
   return relPath.toString();
}

js_Element_c.isRepeatTag = function() {
   if (this.replaceWith !== null)
      return this.replaceWith.isRepeatTag();

   return this.repeat !== null || sc_instanceOf(this, js_IRepeatWrapper);
}

js_HTMLElement_c.runStopScript = function() {
   if (this.stopScript != null && this.initScriptRun === true) {
      this.initScriptRun = false;
      return new Function(this.stopScript).call(this);
   }
}

js_HTMLElement_c.runInitScript = function() {
   if (this.initScript != null) {
      sc_addScheduledJob(this,
                          function initWrapper() {
                              this.initScriptRun = true;
                              new Function(this.initScript).call(this);
                        }, 1, false);
   }
}

// Hide this property in the editor - (for the same annotation on Element in Java)
js_Element_c._PT = {parentNode:{EditorSettings:{visible:false}}};

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

js_HTMLElement_c.getEnclosingTag = function() {
   if (this.parentNode != null)
      return this.parentNode;
   var outer = this.outer;
   while (outer !== undefined) {
      if (sc_instanceOf(outer, js_HTMLElement_c))
         return outer;
      outer = outer.outer;
   }
   return null;
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
      // If we have not been initialized yet, don't bother invalidating.  This gets set before even
      // the id of the element has been defined so too early to decide if we need a refresh.
      if (this.initState === 1) {
         var enclTag = this.getEnclosingTag();
         if (enclTag != null)
            enclTag.invalidateBody();
         else
            this.invalidate();
      }
      sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "visible" , vis);
   }
}

js_HTMLElement_c.isVisible = function() {
   return this.visible;
}

js_HTMLElement_c.isVisibleInView = function() {
   if (!this.visible)
      return false;
   var par = this.getEnclosingTag();
   if (par != null)
      return par.isVisibleInView();
   if (this.isPageElement())
      return true;
   else {
      // Tag not attached to hierarchy but not a 'Page' type so it's not visible
      // TODO: do we need a warning or debug message here?
      return false;
   }
}

js_HTMLElement_c.setBodyOnly = function(nbo) {
   if (this.bodyOnly == nbo)
      return;
   this.bodyOnly = nbo;
   if (this.initState === 1) {
      var enclTag = this.getEnclosingTag();
      if (enclTag != null)
         enclTag.invalidateBody();
      else
         this.invalidate();
   }
   sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "bodyOnly" , nbo);
}

js_HTMLElement_c.getBodyOnly = function() {
   return this.bodyOnly;
}

js_HTMLElement_c.setInitScript = function(scr) {
   this.initScript = scr;
}

js_HTMLElement_c.getInitScript = function() {
   return this.initScript;
}

js_HTMLElement_c.setStopScript = function(scr) {
   this.stopScript = scr;
}

js_HTMLElement_c.getStopScript = function() {
   return this.stopScript;
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

js_HTMLElement_c.setChangedCount = function(ct) {
   if (ct != this.changedCount) {
      this.changedCount = ct;
      sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "changedCount" , this.changedCount);
   }
}

js_HTMLElement_c.getChangedCount = function() {
   return this.changedCount;
}

js_HTMLElement_c.notifyChanged = function() {
   this.setChangedCount(this.changedCount + 1);
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
   if (this.replaceWith !== null) {
      return this.replaceWith.removeFromDOM();
   }
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
   this.startValid = true;
   this.bodyValid = true;
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
   if (this.stopScript)
      this.runStopScript();
   var origElem = this.element;
   this.element = null;
   this.domChanged(origElem, null);
   this.removeAttributeListener();
   this.visible = false; // Mark invisible without sending event just to cancel any pending refreshes
   if (this.repeat != null) {
      this.destroyRepeatTags();
   }
   if (this.replaceWith != null) {
      this.replaceWith = null; // TODO: anything else we need to do here?
   }
   if (this.repeatListener) {
      sc_Bind_c.removeListener(this, "repeat", this.repeatListener, sc_IListener_c.VALUE_INVALIDATED);
      this.repeatListener = null;
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

// The properties of the tagObject we listen to for changes.  When they change, we'll update the backing DOM element
js_HTMLElement_c.refreshAttNames = ["class", "style", "repeat", "replaceWith"];
// The list of attributes which change due to user interaction on the client that we sync to the server for 'serverTags'
js_HTMLElement_c.eventAttNames = [];
// The set of attributes when their value goes to null or "" the attribute name itself is removed
js_HTMLElement_c.removeOnEmpty = {};

var sc$rootTags = new Object();
var sc$rootTagsArray = [];

function sc_refresh() {
   if (!js_Element_c.needsRefresh) {
      js_Element_c.needsRefresh = true;
      if (!js_Element_c.globalRefreshScheduled && !js_Element_c.anyRefreshScheduled) {
         if (js_Element_c.verbose)
            console.log("Scheduling non-page based refresh");
         setTimeout(sc_checkRefresh, 1);
      }
      else if (js_Element_c.verbose)
         console.log("Needs refreshBindings");
   }
}

function sc_checkRefresh() {
   if (!js_Element_c.needsRefresh)
      return;
   js_Element_c.needsRefresh = false;
   if (sc$rootTagsArray.length === 0) {
      if (typeof sc_SyncManager_c !== "undefined") {
         var serverTagMgr = sc_SyncManager_c.getSyncInst("sc.js.PageServerTagManager");
         if (serverTagMgr) { // If there are server tags and no top-level page object - it's entirely a server tag page object.   Create a pageObj stub to refresh the server tags.
            if (js_Element_c.verbose)
               console.log("Initializing server tags only page");
            var pageObj = new js_HtmlPage();
            pageObj.serverContent = true;
            pageObj.refreshServerTags();
            sc$rootTagsArray.push(pageObj);
         }
      }
   }
   for (var i = 0; i < sc$rootTagsArray.length; i++) {
      var rootTag = sc$rootTagsArray[i];
      if (rootTag.refreshBindings) { // When bindings in a tag need to be manually refreshed, you can set 'refreshBindings=true' on the root tag and we'll refresh all bindings on the page
         if (js_Element_c.verbose)
            console.log("Refresh bindings for page: " + rootTag.$protoName);
         sc_Bind_c.refreshBindings(rootTag);
      }
   }

   // If the focus element has changed, update the tag object. TODO: Maybe we should be doing this in a focus event handler so we don't wait for the next refresh?
   var activeElem = document.activeElement;
   var activeTag = null;
   if (activeElem && activeElem.scObj)
      activeTag = activeElem.scObj;
   js_Document_c.getDocument().setActiveElement(activeTag);
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

js_HTMLElement_c.getDOMElement = function() {
   var newElement = null;
   if (this.replaceWith !== null) {
       return this.replaceWith.getDOMElement();
   }
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
   }
   return newElement;
}

js_HTMLElement_c.updateDOM = function() {
   if (this.replaceWith !== null) {
      this.replaceWith.updateDOM();
      this.initState = 1;
   }
   else if (this.repeat === null || this.repeat === undefined) {
      var newElement = this.getDOMElement();
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
      this.initState = 1;
   }
   this.startValid = true;
   this.bodyValid = true;
}

js_HTMLElement_c.updateFromDOMElement = js_HTMLElement_c.setDOMElement = function(newElement) {
   if (newElement !== this.element) {
      var orig = this.element;
      if (orig !== null) {
          if (this.stopScript)
             this.runStopScript();
          delete orig.scObj;
      }

      this.element = newElement;
      if (orig === null && newElement !== null) {
          this.addAttributeListener();
      }

      if (newElement !== null) {
         //sc_id(newElement); if we call this here, it's useful for debugging to make it easier to identify unique elements
         if (newElement.scObj !== undefined) {
            console.log("Warning: replacing object: " + sc_DynUtil_c.getInstanceId(newElement.scObj) + " with: " + sc_DynUtil_c.getInstanceId(this) + " for tag: " + this.tagName);
         }
         newElement.scObj = this;
         if (this.initScript)
            this.runInitScript();
      }
      this.domChanged(orig, newElement);
   }
   this.updateChildDOMs();
   this.initState = 1;
}

js_HTMLElement_c.addAttributeListener = function() {
   if (this._attListener !== undefined)
      return;

   // TODO: performance: we could keep track of which domEvents and attNames are used by each tag, maybe in a bitmask so we could cut down on the overhead of this method
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
      if (this._resizeListener) {
         sc_resizeObserver.unobserve(origElem);
         this._resizeListener = false;
      }
   }
   if (newElem !== null) {
      var curListeners = this.getDOMEventListeners();
      if (curListeners != null) {
         if (this._eventListeners != null) 
             console.log("*** error: replacing element event listeners");
         for (var i = 0; i < curListeners.length; i++) {
            var listener = curListeners[i];

            if (!listener.alias) {
               if (this[listener.propName] === undefined)
                  this[listener.propName] = null; // set the event property to null initially the first time we have someone listening on it.  this is too late but do we want to initialize all of these fields to null on every tag object just so they are null, not undefined?   Just do not override an existing value or refreshBinding fires when we do not want it to
            }
            else {// Now that we know the value of the aliased property (e.g. innerHeight) we need to send a change event cause it changes once we have an element.
               sc_Bind_c.sendChangedEvent(this, listener.propName);
               var ops = listener.otherProps;
               if (ops) {
                  for (var opi = 0; opi < ops.length; opi++) {
                     sc_Bind_c.sendChangedEvent(this, ops[opi]);
                  }
               }
            }

            // Only IE supports the resize event on objects other than the window.
            if (listener.eventName === "resize" && !newElem.attachEvent) {
               sc_addEventListener(window, listener.eventName,
                   function(evt) {
                      if (newElem.scObj) // If not yet attached to the DOM don't bother with this call
                         js_HTMLElement_c.processEvent.call(window, newElem, evt, listener);
                   }
               );
            }
            sc_addEventListener(newElem, listener.eventName, listener.callback);
         }
         this._eventListeners = curListeners;

      }
      this.initResizeListener(newElem);
      var style = this.style;
      if (style != null && style != newElem.getAttribute("style"))
         newElem.setAttribute("style", this.style);
      var doc = js_Document_c.getDocument();
      if (doc.activeElement == this)
         newElem.focus();
   }
   this.notifyChanged();
}

js_HTMLElement_c.initResizeListener = function(newElem) {
   var listeners = sc_getBindListeners(this);
   for (var prop in listeners) {
      if (prop != null && listeners.hasOwnProperty(prop)) {
         if (js_HTMLElement_c.resizeProps.includes(prop)) {
            if (!this._resizeListener) {
               this._resizeListener = true;
               if (typeof ResizeObserver === "function") {
                  if (sc_resizeObserver == null) {
                     sc_resizeObserver = new ResizeObserver(
                        function(entries) {
                           for (var i = 0; i < entries.length; i++) {
                              var elem = entries[i].target;
                              sc_checkSizeProps(elem);
                           }
                        }
                     );
                  }
                  sc_resizeObserver.observe(newElem);
               }
               else {
                  // TODO: in addition to this, we also need to maintain a list of observed elements and either use
                  // mutation handler or after a refreshTags, just run a method to check the size of all observed elements.
                  sc_addEventListener(window, "resize",
                     function(evt) {
                       sc_checkSizeProps(newElem);
                     }
                  );
               }
               // Set initial values and send change events
               sc_checkSizeProps(newElem);
            }
         }
      }
   }
}

function sc_checkSizeProps(elem) {
   var scObj = elem.scObj;
   if (scObj) {
      var listeners = sc_getBindListeners(scObj);
      for (var prop in listeners) {
         if (prop != null && listeners.hasOwnProperty(prop)) {
            if (js_HTMLElement_c.resizeProps.includes(prop)) {
               var newVal = elem[prop];
               if (scObj[prop] != newVal) {
                  scObj[prop] = newVal;
                  sc_Bind_c.sendChange(scObj, prop, newVal);
               }
            }
         }
      }
   }
}

js_HTMLElement_c.initDOMListener = function(listener, prop, scEventName) {
   if (scEventName == null)
      scEventName = prop;
   else
      listener.alias = true; // This is like clientWidth which is mapped to a separate resizeEvent
   // Convert from the sc event name, e.g. clickEvent to click
   var jsEventName = scEventName === "mouseDownMoveUp" ? "mousedown" : scEventName.substring(0, scEventName.indexOf("Event")).toLowerCase();
   listener.eventName = jsEventName;
   listener.scEventName = scEventName;
   listener.propName = prop;
   listener.callback = sc_newEventArgListener(js_HTMLElement_c.eventHandler, listener);
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

// For the given HTMLElement instance, return the array of dom event listeners (dom listeners).  The domEvents like clickEvent, are converted to properties
// of the tag object.  We do this lazily in the browser for speed, and only create and set the properties for the events which someone is listening to.
// This means we do need to listen for the 'addListener' binding event so when a new listener is added, we can start listening on the appropriate dom
// DOM event property.
js_HTMLElement_c.getDOMEventListeners = function() {
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
               if (eventNameList.constructor === Array) {
                  for (var i = 0; i < eventNameList.length; i++) {
                     var nextEventName = eventNameList[i];
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
      }
   }
   return res;
}

js_HTMLElement_c.click = function() {
   var evt = new MouseEvent("click");
   evt.currentTarget = evt.target = this; // TODO: verify that this works. It seems like this properties may not be settable in JS
   this.clickEvent = evt;
   sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "clickEvent", evt);
}

js_HTMLElement_c.setReplaceWith = function(rw) {
   var oldRW = this.replaceWith;
   if (oldRW === rw)
      return;
   if (oldRW === null)
      oldRW = this;
   // Clear out the old DOM element
   oldRW.updateFromDOMElement(null);

   this.replaceWith = rw;
   sc_Bind_c.sendChangedEvent(this, "replaceWith");
   this.invalidate();

   // On refresh, if set the replaceWith element attaches to the new DOM element
}

js_HTMLElement_c.getReplaceWith = function() {
   return this.replaceWith;
}

js_HTMLElement_c.initRepeatListener = function() {
   if (!this.repeatListener) {
      this.repeatListener = new js_RepeatListener(this);
      sc_Bind_c.addListener(this, "repeat", this.repeatListener, sc_IListener_c.VALUE_INVALIDATED);
   }
}

js_HTMLElement_c.setRepeat = function(r) {
   var oldR = this.repeat;
   if (oldR === r)
      return;
   if (this.repeatListener && sc_instanceOf(oldR, sc_IChangeable)) {
      sc_Bind_c.removeListener(oldR, null, this.repeatListener, sc_IListener_c.VALUE_INVALIDATED);
   }
   if (r !== null) {
      this.initRepeatListener();
      if (sc_instanceOf(r, sc_IChangeable))
         sc_Bind_c.addListener(r, null, this.repeatListener, sc_IListener_c.VALUE_INVALIDATED);
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
   if (v != this.repeatVar) {
      this.repeatVar = v;
      sc_Bind_c.sendChangedEvent(this, "repeatVar");
   }
}

js_HTMLElement_c.getRepeatVar = function() {
   return this.repeatVar;
}

js_HTMLElement_c.setRepeatIndex = function(v) {
   if (v != this.repeatIndex) {
      this.repeatIndex = v;
      sc_Bind_c.sendChangedEvent(this, "repeatIndex");
   }
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

js_HTMLElement_c.initChildren = function() {
   if (this.repeat !== null) {
      this.syncRepeatTags(false);
   }
}

js_HTMLElement_c.anyChangedRepeatTags = function() {
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

// Remove all sub tags and rebuild from scratch. Unlike refreshRepeat, this does not do an incremental update.
js_HTMLElement_c.rebuildRepeat = function() {
   this.destroyRepeatTags();
   this.refreshRepeat(false);
}

js_HTMLElement_c.refreshRepeat = function(noRefresh) {
   if (this.repeat && this.syncRepeatTags(true)) {
      if (!noRefresh)
         this.refreshBody();
      // This is called on the repeatWrapper which does not have an dom element so we need to refresh the parent tag
      else
         this.getEnclosingTag().invalidateBody();
   }
}

js_HTMLElement_c.repeatTagsChanged = function() {
   this.notifyChanged();
}

// This method gets called when the repeat property has changed.  For each value in the repeat array we create a tag object and corresponding DOM element in repeatTags.
// process incrementally updates one from the other, trying to move and preserve the existing tags when possible because that will lead to incremental UI refreshes.
js_HTMLElement_c.syncRepeatTags = function(updateDOM) {
   var needsRefresh = false;
   var repeat = this.repeat;
   var anyChanges = false;
   //var oldSyncState = sc_SyncManager_c.getSyncState();
   try {
      //sc_SyncManager_c.setSyncState(sc_SyncManager_SyncState_c.Disabled);

      if (repeat === undefined)
         console.error("Undefined value for repeat property on: " + this);

      var sz = repeat === null ? 0 : sc_DynUtil_c.getArrayLength(repeat);
      if (repeat !== null) {
         this.repeatTagsValid = true;
         // Delay the creation until we are visible unless we've already sync'd the repeat property against the repeat tags.
         if (!this.isVisibleInView()) {
            if (js_Element_c.verboseRepeat) {
               console.log("syncRepeatTags - invisible tag: repeatTags for: " + this.id);
               this.dumpRepeatTags();
            }
            this.destroyRepeatTags();
            anyChanges = true;
            return false;
         }
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
               needsRefresh = anyChanges = true;
            }
            if (js_Element_c.verboseRepeat) {
               console.log("syncRepeatTags - new repeat repeatTags for: " + this.id);
               this.dumpRepeatTags();
            }
         }
         // Incrementally update the tags while keeping the DOM synchronized with the object tree
         else {
            if (js_Element_c.verboseRepeat) {
               console.log("syncRepeatTags - updating with new size: " + sz + " and old size: " + repeatTags.length + " for: " + this.id);
               this.dumpRepeatTags();
            }
            var renumberIx = -1;
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
                     anyChanges = true;
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
                              renumberIx = delIx;
                           }
                           else
                              needsMove = true;
                        }
                        // If we deleted up to the current, we are done.  Otherwise, we need to re-order
                        if (needsMove) {
                           elemToMove.setRepeatIndex(i);
                           needsRefresh = this.moveElement(elemToMove, curIx, i, updateDOM) || needsRefresh;
                           renumberIx = i;
                        }
                     }
                  }
               }
               else {
                  anyChanges = true;
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
               anyChanges = true;
               
               if (js_Element_c.verboseRepeat) {
                  console.log("syncRepeatTags - removing end nodes - new size: " + sz + " old size: " + repeatTags.length + " for: " + this.id);
                  this.dumpRepeatTags();
               }
               var ix = repeatTags.length - 1;
               var toRem = repeatTags[ix];
               needsRefresh = this.removeElement(toRem, ix, updateDOM) || needsRefresh;
            }
            if (renumberIx != -1) {
               var tagSz = repeatTags.length;
               for (var r = renumberIx; r < tagSz; r++) {
                  var tagElem = repeatTags[r];
                  if (tagElem.getRepeatIndex() != r)
                     tagElem.setRepeatIndex(r);
               }
               if (sc_instanceOf(this, js_IRepeatWrapper)) {
                  this.updateElementIndexes(renumberIx);
               }
            }
         }

         if (js_Element_c.verboseRepeat) {
            console.log("Finished sync repeat: ");
            this.dumpRepeatTags();
         }
      }
      else {
         if (js_Element_c.verboseRepeat && this.repeatTags != null) {
            console.log("syncRepeatTags - no array value - destroying repeat tags: ");
            this.dumpRepeatTags();
         }
         this.destroyRepeatTags();
      }
   }
   finally {
      if (anyChanges)
         this.repeatTagsChanged();
      //sc_SyncManager_c.setSyncState(oldSyncState);
   }
   return needsRefresh;
}

js_HTMLElement_c.dumpRepeatTag = function(tag) {
    if (tag.formEditor != null)
        console.log("  " + tag.id + " scId: " + sc_id(tag) + " form editor: " + tag.formEditor.id);
    else if (tag.parentEditor != null)
        console.log("  " + tag.id + " scId: " + sc_id(tag) + " parent editor: " + tag.parentEditor.id);
    else
        console.log("  " + tag.id + " scId: " + sc_id(tag));
}

js_HTMLElement_c.dumpRepeatTags = function() {
   if (this.repeatTags != null) {
      console.log("element: " + this.id + " has: " + this.repeatTags.length + " repeatTags");
      for (var i = 0; i < this.repeatTags.length; i++) { 
          var tag = this.repeatTags[i];
          this.dumpRepeatTag(tag);
      }
   }
   else
      console.log("element: " + this.id + " has null repeatTags");
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
      var outRes = tag.output().toString();
      // If there are no contents here tmp.childNodes is an empty array.  This happens at least when
      // the tag is invisible
      if (outRes.length > 0) {
         tmp.innerHTML = outRes;
         // We want to append this element after curElem but there's only "insert before" in the DOM api
         var nextElem = curElem.nextSibling;
         var parentNode = curElem.parentNode;
         if (nextElem === null)
            parentNode.appendChild(tmp.childNodes[0]);
         else
            parentNode.insertBefore(tmp.childNodes[0], nextElem);
      }
      //document.removeChild(tmp); ?? needs to be removed somehow
   }
   this.repeatTags.push(tag);
   tag.updateDOM();
   return false;
}

js_HTMLElement_c.insertElement = function(tag, ix, updateDOM) {
   if (js_Element_c.verboseRepeat) {
      console.log("insertElement:" + ix);
      this.dumpRepeatTag(tag);
   }
   var needsRefresh = false;
   if (updateDOM) {
       // Can't do an incremental refresh when there is no current element... to do this incrementally maybe we insert a dummy tag when we remove the last one?
       if (ix >= this.repeatTags.length) {
          if (ix == this.repeatTags.length)
             this.repeatTags.push(tag);
          else
             console.error("Warning - not adding element onto repeatTags");
          return true;
       }
       var tmp = document.createElement('div');
       var outRes = tag.output().toString();
       tmp.innerHTML = outRes;
       var repeatTag = this.repeatTags[ix];
       var curElem = repeatTag.element;
       if (curElem == null) {
          repeatTag.updateDOM();
          curElem = repeatTag.element;
          if (curElem == null) {
             needsRefresh = true;
          }
       }
       if (!needsRefresh && outRes.length > 0)
          curElem.parentNode.insertBefore(tmp.childNodes[0], curElem);
       //document.removeChild(tmp); ?? does this need to be removed - it throws an exception?
   }
   this.repeatTags.splice(ix, 0, tag);
   tag.updateDOM();
   return needsRefresh;
}

js_HTMLElement_c.removeElement = function(tag, ix, updateDOM) {
   var needsRefresh = false;
   if (js_Element_c.verboseRepeat) {
      console.log("removeElement:" + ix);
      this.dumpRepeatTag(tag);
   }
   if (updateDOM) {
      var repeatTag = this.repeatTags[ix];
      var curElem = repeatTag.element;
      if (curElem == null) {
         repeatTag.updateDOM();
         curElem = repeatTag.element;
         if (curElem == null) { // maybe we were invisible for the last change of the list?
            needsRefresh = true;
         }
      }
      if (this.bodyOnly) // TODO: more than one tag in the body - should be able to 'rewind' by figuring out how many children per repeat element?
         needsRefresh = true;
      if (!needsRefresh)
         curElem.parentNode.removeChild(curElem);
   }
   this.repeatTags.splice(ix,1);
   // Needs to be done after updateDOM as it sets the element = null.
   tag.destroy();
   return needsRefresh;
}

js_HTMLElement_c.moveElement = function(tag, oldIx, newIx, updateDOM) {
   if (js_Element_c.verboseRepeat) {
      console.log("moveElement - " + oldIx + " -> " + newIx);
      this.dumpRepeatTag(tag);
   }
   var needsRefresh = false;
   if (updateDOM) {
      var elemToMove = tag.element;
      if (elemToMove == null) {
         tag.updateDOM();
         elemToMove = tag.element;
         if (elemToMove == null) {
            needsRefresh = true;
         }
      }
      if (this.bodyOnly) // TODO: do this incrementally?
         needsRefresh = true;
      // The new spot is at the end of the list
      if (!needsRefresh) {
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
   }
   if (this.repeatTags[oldIx] === tag)
      this.repeatTags.splice(oldIx, 1);
   else
      console.error("Unrecognized case in moveElement for syncRepeatTags");
   this.repeatTags.splice(newIx, 0, tag);

   return needsRefresh;
}

js_HTMLElement_c.getId = function() {
   if (this.id != null)
      return this.id;
   if (this.element == null)
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

js_HTMLElement_c.getAltChildren = function() {
   if (!this.getObjChildren)
      return null;
   var children = this.getObjChildren(true);
   if (children == null)
      return null;
   var res = null;
   for (var i = 0; i < children.length; i++) {
      var child = children[i];
      var ix;
      if (child.getId) {
         var id = child.getId();
         var ix = id.indexOf("__alt");
         if (ix != -1 && ix == id.length - 5) {
            if (res == null) res = [];
            res.push(child);
         }
      }
   }
   return res;
}

js_HTMLElement_c.refresh = function() {
   if (this.serverContent) {
      if (this.element == null) { // Attach to the dom generated by the server
         this.startValid = true;
         this.bodyValid = true;
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
      if (sc_PTypeUtil_c.testMode || js_Element_c.verbose)
         sc_log("New root tag element: " + this.$protoName);
   }
   else
      console.log("Warning: second root tag: " + this.$protoName);
}

js_HTMLElement_c.refreshBodyContent = function(sb) {
   return this.outputBody(sb);
}

js_HTMLElement_c.refreshBody = function() {
   var create = false;
   if (this.replaceWith !== null) {
      var replTag = this.replaceWith;
      replTag.parentNode = this.parentNode;
      replTag.refreshBody();
      this.bodyValid = replTag.bodyValid;
      return;
   }

   this.updateDOM();
   if (this.element === null) {
      // We were made invisible or possibly our parent is invisible
      if (!this.isVisibleInView()) {
         this.bodyValid = true;
         return;
      }
      // Go to the top level tag and start the output process up there.  It needs to attach us to the tree
      var par = this.getEnclosingTag();
      if (par != null && par.refreshBody !== null) {
         if (js_Element_c.trace)
            console.log("refreshBody of: " + this.id + " refreshing parent body: " + par.id);
         par.refreshBody();
         return;
      }
      else {
         create = true;
      }
   }
   this.bodyValid = true;

   var par = this.getEnclosingTag();
   if (par == null && this.element == null) {
      if (this.isPageElement())
         this.makeRoot();
      else {
         console.error("Attempt to refresh tag not attached where parentNode is not defined")
         return;
      }
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
               console.error("Error appending child for refreshBody of element: " + this.id + + " tag: " + this.tagName + " error: " + e + " for content: " + outRes);
            }
         }
         else {
            document.write(outRes);
         }
         this.updateDOM();
      }
      else {
         // TODO: are there any cases where this is not the right thing?  Like blanking out some content we need to blank out?
         console.log("Ignoring empty body on create");
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
   if (!this.visible) {
      this.removeFromDOM();
   }
   else if (this.element) {
      var sb = new jv_StringBuilder();
      this.outputStartTag(sb);
      this.setStartTagTxt(sb.toString());
   }
   // else - TODO: will the parent always be refreshed in this case or do we need to re-render the entire element under a temporary one and use replaceChild here
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

js_HTMLElement_c.output = js_HTMLElement_c.output_c = function() {
   var sb = new jv_StringBuilder();
   this.outputTag(sb);
   return sb;
}

js_HTMLElement_c.focus = function() {
   if (this.element)
      this.element.focus();
   js_Window_c.getWindow().documentTag.setActiveElement(this);
}

js_HTMLElement_c.createRepeatElement = function(rv, ix, oldTag) {
   var elem;
   var sync = typeof sc_SyncManager_c != "undefined";
   var flush = sync && sc_SyncManager_c.beginSyncQueue();
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
   if (this.wrap)
      elem.bodyOnly = true;
   elem.parentNode = this;
   if (elem != null && sync)
      js_HTMLElement_c.registerSyncInstAndChildren(elem);
   if (flush)
       sc_SyncManager_c.flushSyncQueue();

   if (js_Element_c.verboseRepeat) {
      if (rv == elem)
         console.log("Reusing original repeat element: ");
      else
         console.log("Created repeat element: ");
      this.dumpRepeatTag(elem);
   }
   return elem;
}

js_HTMLElement_c.updateElementIndexes = function(ix) {
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

js_HTMLElement_c.markBodyValid = function(v) {
   this.bodyValid = v; 
}

js_HTMLElement_c.markStartTagValid = function(v) {
   this.startValid = v;
}


js_HTMLElement_c.serverContent = false;

js_HTMLElement_c.outputTag = function(sb) {
   if (!this.visible) {
      var invisTags = this.invisTags;
      if (invisTags == null) {
         invisTags = this.getAltChildren();
         if (invisTags == null)
            invisTags = [];
      }
      for (var i = 0; i < invisTags.length; i++)
         invisTags[i].outputTag(sb);
      return;
   }
   if (this.replaceWith !== null) {
      this.replaceWith.outputTag(sb);
      return;
   }
   if (this.repeat !== null) {
      this.syncRepeatTags(true);
      if (this.wrap)
         this.outputStartTag(sb);
      if (this.repeatTags !== null) {
         for (var i = 0; i < this.repeatTags.length; i++) {
            var rtag = this.repeatTags[i];
            rtag.outputTag(sb);
            rtag.startValid = true;
            rtag.bodyValid = true;
         }
      }
      if (this.wrap)
         this.outputEndTag(sb);
   }
   else {
      if (!this.bodyOnly)
         this.outputStartTag(sb);
      if (this.serverContent) {
         if (this.element != null)
            sb.append(this.element.innerHTML);
         else
            console.error("Missing HTML for server tag object: " + this.getId() + " use style='display:none' instead of visible=false on the initial page load.");
      }
      else
         this.outputBody(sb);
      if (!this.bodyOnly)
         this.outputEndTag(sb);
   }
}

js_HTMLElement_c.invalidate = function() {
   this.invalidateStartTag();
   this.invalidateBody();
}

js_HTMLElement_c.schedRefresh = function() {
   if (!this.refreshScheduled) {
      if (!js_Element_c.globalRefreshScheduled) {
         if (!js_Element_c.anyRefreshScheduled) {
            js_Element_c.anyRefreshScheduled = true;
            sc_addRunLaterMethod(this, js_Element_c.refreshTags, 5);
         }
         js_Element_c.tagsToRefresh.push(this);
      }
   }
}

js_HTMLElement_c.invalidateBody = function() {
   this.bodyValid = false;
   if (this.initState === 1)
      this.schedRefresh();
}

js_HTMLElement_c.invalidateStartTag = function() {
   this.startValid = false;
   if (this.initState === 1)
      this.schedRefresh();
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
   else {
      this.repeatTagsValid = false;
      this.schedRefresh();
   }
}

// Specifies the standard DOM events - each event can specify a set of alias properties.  A 'callback' function is lazily added to each domEvent entry the first time we need to listen for that DOM event on an object
js_HTMLElement_c.domEvents = {clickEvent:{}, dblClickEvent:{}, mouseDownEvent:{}, mouseMoveEvent:{}, mouseDownMoveUp:{},
                               mouseOverEvent:{aliases:["hovered"], computed:true}, mouseOutEvent:{aliases:["hovered"], computed:true}, 
                               mouseUpEvent:{}, keyDownEvent:{}, keyPressEvent:{}, keyUpEvent:{}, submitEvent:{}, changeEvent:{}, blurEvent:{}, focusEvent:{}};

// the reverse direction for the aliases field of the domEvent entry
js_HTMLElement_c.domAliases = {hovered:["mouseOverEvent","mouseOutEvent"]};

js_HTMLElement_c.resizeProps = ["clientWidth","clientHeight","offsetWidth","offsetHeight","scrollWidth","scrollHeight"];

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
   if (elem === document && listener.mouseDownElem) {
      elem = listener.mouseDownElem;
   }
   js_HTMLElement_c.processEvent(elem, event, listener);
}

js_HTMLElement_c.processEvent = function(elem, event, listener) {
   var scObj = elem.scObj;
   if (scObj !== undefined) {
      var ops = listener.otherProps;

      // Add this as a separate field so we can use the exposed parts of the DOM api from Java consistently
      event.currentTag = scObj;
      var eventValue, otherEventValues = null;

      // e.g. clientWidth or hovered - properties computed from other DOM events
      if (listener.alias != null) {
         // e.g. hovered which depends on mouseOut/In - just fire the change event as the property is changed in the DOM api
         var computed = listener.computed;
         var origValue = null;
         var otherOrigValue = null;
         if (computed) {
            origValue = sc_DynUtil_c.getPropertyValue(scObj, listener.propName);
            // The getX method (e.g. getHovered) needs the info from the event to compute it's value properly
            scObj[listener.scEventName] = event;
            // Not checking otherPropName here because we never have computed and more than one alias
         }
         // Access this for logs and so getHovered is called to cache the value of "hovered"
         eventValue = sc_DynUtil_c.getPropertyValue(scObj, listener.propName);
         if (ops) {
            otherEventValues = [];
            for (var opi = 0; opi < ops.length; opi++)
               otherEventValues.push(sc_DynUtil_c.getPropertyValue(scObj, ops[opi]));
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

      if (js_Element_c.trace && listener.scEventName !== "mouseMoveEvent")
         console.log("tag event: " + listener.propName + ": " + listener.scEventName + " = " + eventValue);
      sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, scObj, listener.propName, eventValue);
      if (ops) {
         for (opi = 0; opi < ops.length; opi++) {
            sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, scObj, ops[opi], otherEventValues[opi]);
         }
      }

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
      console.log("Unable to find scObject to update in eventHandler");
}


js_HTMLElement_c.getClientWidth = function() {
   if (this.element == null)
      return 0;
   return this.element.clientWidth;
}

js_HTMLElement_c.getClientHeight = function() {
   if (this.element == null)
      return 0;
   return this.element.clientHeight;
}

js_HTMLElement_c.getScrollWidth = function() {
   if (this.element == null)
      return 0;
   return this.element.scrollWidth;
}

js_HTMLElement_c.getScrollHeight = function() {
   if (this.element == null)
      return 0;
   return this.element.scrollHeight;
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
   js_HTMLElement.apply(this, arguments);
   this.value = null;
   this.type = "text";
   this.clickCount = 0;
   this.checked = false;
   this.tagName = "input";
   this.disabled = null;
   this.size = 20;
   this.liveEdit = "on";
   this.liveEditDelay = 0;
}
js_Input_c = sc_newClass("sc.lang.html.Input", js_Input, js_HTMLElement, null);

js_Input_c.refreshAttNames = js_HTMLElement_c.refreshAttNames.concat(["value", "disabled", "checked", "size"]);
js_Input_c.eventAttNames = js_HTMLElement_c.eventAttNames.concat(["value", "checked", "changeEvent", "clickCount"]);
js_Input_c.removeOnEmpty = {value:true};

//js_Input_c.booleanAtts = {checked:true};

// Warning - this is not set for this function - it's called directly by the event handler
js_Input_c.doChangeEvent = function(event) {
   var elem = event.currentTarget ? event.currentTarget : event.srcElement;
   var scObj = elem.scObj;
   if (scObj !== undefined) {
      var cs = false;
      // If liveEdit is off - just trigger the change without triggering a sync.
      // If it is set to 'change' - only the change event triggers the sync.
      var liveEditChange = scObj.liveEdit == "change";
      if ((scObj.liveEdit == "off" ||
           scObj.liveEditDelay != 0 ||
           (liveEditChange && event.type == "keyup")) && typeof sc_ClientSyncManager_c !== "undefined") {
          sc_ClientSyncManager_c.syncDelaySet = true;
          sc_ClientSyncManager_c.currentSyncDelay = scObj.liveEditDelay != 0 ? scObj.liveEditDelay : -1;
          cs = true;
      }
      if (scObj.setValue) {
         scObj.setValue(this.value);
         if (scObj.value == "" && scObj.removeOnEmpty.value != null)
            this.removeAttribute("value");
      }
      if (scObj.setChecked) {
         scObj.setChecked(this.checked);
         // If this is a radio type it will have cleared the checked on the other buttons when we set this one
         // so we need to clear them in the wrapper to keep them in sync.
         if (this.checked && this.type == "radio") {
            var name = this.name;
            var rl = document.getElementsByName(name);
            if (rl !== null) {
               for (var i = 0; i < rl.length; i++) {
                  var rde = rl[i];
                  if (rde !== this) {
                     var rtag = rde.scObj;
                     if (rtag && !rde.checked)
                        rtag.setChecked(false);
                  }
               }
            }
         }
      }

      if (cs) {
         sc_ClientSyncManager_c.syncDelaySet = false;
      }
      // Force a change event here in case the value change was sent on keyup and did not force a sync.
      else if (liveEditChange && event.type == "change" && typeof sc_SyncManager_c !== "undefined")
         sc_SyncManager_c.getDefaultSyncContext().markChanged();

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
   if (origElem !== null) {
      sc_removeEventListener(origElem, 'change', js_Input_c.doChangeEvent);
      sc_removeEventListener(origElem, 'keyup', js_Input_c.doChangeEvent);
   }
   if (newElem !== null) {
      sc_addEventListener(newElem, 'change', js_Input_c.doChangeEvent);
      sc_addEventListener(newElem, 'keyup', js_Input_c.doChangeEvent);
      if (this.value != null && !this.serverContent && this.type != "file")
         newElem.value = this.value; 
   }
   // Putting this after the above listener which updates 'value'
   // because JS will use this order for event callbacks and we want
   // value to be set in the changeEvent and keyUpEvent handlers.
   js_HTMLElement_c.domChanged.call(this, origElem, newElem);
}

js_Input_c.updateFromDOMElement = function(newElem) {
   js_HTMLElement_c.updateFromDOMElement.call(this, newElem);
   this.value = newElem.value;
   this.checked = newElem.checked;
}

js_Input_c.setValue = function(newVal) {
   if (newVal === null)
      newVal = "";
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

js_Input_c.setSize = function(s) {
   if (s != this.size) {
      this.size = s;
      if (this.element !== null && this.element.size != s)
         this.element.size = s;
      sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "size" , s);
   }
}

js_Input_c.getSize = function() {
   return this.size;
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

js_Input_c.setChecked = function(ch) {
   if (ch != this.checked) {
      this.checked = ch; 
      if (this.element !== null && this.element.checked != ch)
         this.element.checked = ch;
      sc_Bind_c.sendChangedEvent(this, "checked");
   }
}

js_Input_c.getChecked = function() {
   return this.checked; 
}

function js_Button() {
   js_Input.call(this);
}

js_Button_c = sc_newClass("sc.lang.html.Button", js_Button, js_Input, null);

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
   js_HTMLElement.apply(this, arguments);
   this.tagName = "a";
   this.clickCount = 0;
   this.disabled = null;
}

js_A_c = sc_newClass("sc.lang.html.A", js_A, js_HTMLElement, null);
js_A_c.domChanged = js_Button_c.domChanged;
js_A_c.doClickCount = js_Button_c.doClickCount;
js_A_c.setClickCount = js_Input_c.setClickCount;
js_A_c.getClickCount = js_Input_c.getClickCount;
js_A_c.setDisabled = js_Input_c.setDisabled;
js_A_c.getDisabled = js_Input_c.getDisabled;

function js_SelectListener(scObj) {
   sc_AbstractListener.call(this);
   this.scObj = scObj;
}

js_SelectListener_c = sc_newClass("sc.lang.html.SelectListener", js_SelectListener, sc_AbstractListener, null);

js_SelectListener_c.valueValidated = function(obj, prop, detail, apply) {
   var scObj = this.scObj;
   scObj.invalidate();
}

function js_Select() {
   js_HTMLElement.apply(this, arguments);
   this.tagName = "select";
   this.optionDataSource = null;
   this.selectedIndex = -1;
   this.selectedValue = null;
   this.disabled = false;
   this.size = 20;
   this.multiple = false;
}
js_Select_c = sc_newClass("sc.lang.html.Select", js_Select, js_HTMLElement, null);
js_Select_c.eventAttNames = js_HTMLElement_c.eventAttNames.concat([ "selectedIndex", "changeEvent"]);

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
   if (origElem != null)
      sc_removeEventListener(origElem, 'change', js_Select_c.doChangeEvent);
   if (newElem != null) {
      sc_addEventListener(newElem, 'change', js_Select_c.doChangeEvent);
      if (!this.serverContent) // when serverContent the tagObject is just using the DOM's value
         newElem.selectedIndex = this.selectedIndex;
   }
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
      if (newDS != null && this.selectedIndex != -1 && this.selectedValue == null && this.selectedIndex < sc_PTypeUtil_c.getArrayLength(newDS)) {
         this.setSelectedValue(sc_PTypeUtil_c.getArrayElement(newDS, this.selectedIndex));
      }
   }
   sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "optionDataSource" , newDS);
}

js_Select_c.getOptionDataSource = function() {
   return this.optionDataSource; 
}

js_Select_c.setMultiple = function(m) {
   if (m != this.multiple) {
      this.multiple = m;
      if (this.element !== null && this.element.multiple != m)
         this.element.multiple = m;
      sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "multiple" , m);
   }
}

js_Select_c.getMultiple = function() {
   return this.multiple;
}

// TODO: the select tag should probably use the "repeat" property which it already inherits.  For now it's an experiment in 
// a different way to implement sub-tags which is not stateful, more code-control by rendering each option after setting the data source.  
// The outputBody method is not really used here directly which means we have to override refreshBody as well.
js_Select_c.outputTag = function(sb) {
   if (!this.visible)
      return;
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
   if (!this.visible)
      return;
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
         sb.append(this.escBody(dv));
         sb.append("</option>");
      }
      else {
         var subOption = defChildren[dix % defChildren.length];
         // disable refresh by marking this invalid before we change the data
         subOption.bodyValid = false;
         subOption.startValid = false;
         subOption.setSelected(selected);
         subOption.setOptionData(dv);
         // TODO: right now the tag itself must render 'selected' but should we have a way to inject that attribute if it's not already specified?
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
   js_HTMLElement.apply(this, arguments);
   this.tagName = "option";
   this.optionData = null;
   this.selected = false;
   this.disabled = null;

}
js_Option_c = sc_newClass("sc.lang.html.Option", js_Option, js_HTMLElement, null);

js_Option_c.refreshAttNames = js_HTMLElement_c.refreshAttNames.concat["selected", "disabled", "value"];
js_Option_c.eventAttNames = js_HTMLElement_c.eventAttNames.concat["selected", "optionData"];

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

js_Select_c.setDisabled = js_Option_c.setDisabled = js_Input_c.setDisabled;
js_Select_c.getDisabled = js_Option_c.getDisabled = js_Input_c.getDisabled;

js_Select_c.setSize = js_Input_c.setSize;
js_Select_c.getSize = js_Input_c.getSize;

function js_Form() {
   js_HTMLElement.apply(this, arguments);
   this.tagName = "form";
   this.submitCount = 0;
   this.submitInProgress = false;
   this.submitError = null;
   this.submitResult = null;
}

js_Form_c = sc_newClass("sc.lang.html.Form", js_Form, js_HTMLElement, null);
js_Form_c.eventAttNames = js_HTMLElement_c.eventAttNames.concat([ "submitCount", "submitEvent"]);

js_Form_c.submitEvent = function(event) {
   var elem = event.currentTarget ? event.currentTarget : js_findCurrentTargetSimple(event.srcElement, "submitCount");
   var scObj = elem.scObj;
   if (scObj !== undefined)
      scObj.setSubmitCount(scObj.getSubmitCount()+1);
   else 
      console.log("Unable to find scObject to update in submitEvent");
}

js_Form_c.setSubmitInProgress = function(v) {
   this.submitInProgress = v;
   sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "submitInProgress" , this.submitInProgress);
}

js_Form_c.getSubmitInProgress = function() {
   return this.submitInProgress;
}

js_Form_c.setSubmitError = function(e) {
   this.submitError = e;
   sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "submitError" , this.submitError);
}

js_Form_c.getSubmitError = function() {
   return this.submitError;
}

js_Form_c.setSubmitResult = function(v) {
   this.submitResult = v;
   sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "submitResult" , this.submitResult);
}

js_Form_c.getSubmitResult = function() {
   return this.submitResult;
}

js_Form_c.submit = function() {
   if (this.element)
      this.element.submit();
   else
      sc_error("No DOM element for form in submit() call");
}

js_Form_c.sendSubmitEvent = function() {
   var evt = new Event('submit');
   evt["currentTarget"] = evt["target"] = this; // not using "." here because intelliJ complains these are constant - will any browsers barf on this?  If so we'll need to just create a new object and copy over any fields we need.
   this.submitEvent = evt;
   sc_Bind_c.sendEvent(sc_IListener_c.VALUE_CHANGED, this, "submitEvent", evt);
}

js_Form_c.submitFormData = function(url) {
   if (this.element) {
      var formData = new FormData(this.element);
      /*
      var enctype = this.element.enctype;
      if (!enctype)
         enctype = "application/x-www-form-urlencoded";
      */
      // TODO: add a progress handler and support size/downloaded properties for the current submission
      var listener = {
         obj:this,
         response: function(r) {
            var res = JSON.parse(r);
            this.obj.setSubmitResult(res.result);
            this.obj.setSubmitError(res.error);
            this.obj.setSubmitInProgress(false);
         },
         error: function(code, msg) {
            this.obj.setSubmitError(msg);
            this.obj.setSubmitInProgress(false);
            sc_error("Error from submitFormData result: " + code + ": " + msg);
         }
      };
      this.setSubmitInProgress(true);
      this.setSubmitResult(null);
      this.setSubmitError(null);
      this.setSubmitCount(this.getSubmitCount()+1);
      sc_PTypeUtil_c.postHttpRequest(url, formData, null, listener);
   }
}

js_Form_c.domChanged = function(origElem, newElem) {
   if (origElem != null)
      sc_removeEventListener(origElem, 'submit', js_Form_c.submitEvent);
   if (newElem != null)
      sc_addEventListener(newElem, 'submit', js_Form_c.submitEvent);
   js_HTMLElement_c.domChanged.call(this, origElem, newElem);
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
   this.updateURLScheduled = false;
   // Schedule a refresh once this script is done loading
   sc_addLoadMethodListener(this, js_Page_c.onPageLoad);
   sc_runLaterScheduled = true;
   this.refreshedOnce = false;
   var pi = js_PageInfo_c.pages[this.$protoName];
   if (pi == null) {
      this.queryParamProperties = null;
      this.urlParts = null;
   }
   else {
      this.queryParamProperties = pi.queryParamProperties;
      this.urlParts = pi.urlParts;
   }
   // Set page properties and add listeners to update the URL state
   this.updatePageFromURL(true);
   // Signal to others not to bother refreshing individually - avoids refreshing individual tags when we are going to do it at the page level anyway
   //if (this.refreshOnLoad)
   //   js_Element_c.globalRefreshScheduled = true;
   this.makeRoot();
   this.serverTags = {}; // element id as the key pointing to the ServerTag instance received from the server
   this.serverTagObjs = {}; // stores wrapper server tag objects using element id as the key.
   this.stInit = false;
   this.urlChanged = false;
}

js_Page_c = sc_newClass("sc.lang.html.Page", js_Page, js_HTMLElement, null);

function js_Div() {
   js_HTMLElement.apply(this, arguments);
   this.tagName = "div";
}
js_Div_c = sc_newClass("sc.lang.html.Div", js_Div, js_HTMLElement, null);

function js_Span() {
   js_HTMLElement.apply(this, arguments);
   this.tagName = "span";
}
js_Span_c = sc_newClass("sc.lang.html.Span", js_Span, js_HTMLElement, null);

function js_Head() {
   js_HTMLElement.call(this);
   this.tagName = "head";
}
js_Head_c = sc_newClass("sc.lang.html.Head", js_Head, js_HTMLElement, null);

function js_Body() {
   js_HTMLElement.call(this);
   this.tagName = "body";
}
js_Body_c = sc_newClass("sc.lang.html.Body", js_Body, js_HTMLElement, null);

function js_Html() {
   js_Page.call(this); // NOTE: In Java Html extends HTMLElement, not Page because (it seems) we can have an html tag that's not a top-level page - TODO: is this right?  Should we have HtmlPage extend Page on both client and server.
   this.tagName = "html";
}

js_Html_c = sc_newClass("sc.lang.html.Html", js_Html, js_HTMLElement, null);

js_Page_c._updateInst = js_Html_c._updateInst = function() {
   // TODO: should we invalidate the body here?
}

function js_HtmlPage() {
   js_Html.call(this);
   this.pageVisitCount = 0;
   //this.pageURL = null;
   this.queryParamProperties = null;
   this.pageJSFiles = null;
}

js_HtmlPage_c = sc_newClass("sc.lang.html.HtmlPage", js_HtmlPage, js_Html, null);

js_HtmlPage_c.isPageElement = js_Page_c.isPageElement = function() { return true; }

js_HtmlPage_c.getPageURL = js_Page_c.getPageURL = function() {
   return this.pageURL;
}

js_HtmlPage_c.setPageURL = js_Page_c.setPageURL = function(url) {
   this.pageURL = url;
}

js_HtmlPage_c.getPageBaseURL = js_Page_c.getPageBaseURL = function() {
   if (this.pageURL == null)
      return null;
   var ix = this.pageURL.indexOf("?");
   if (ix == -1)
      return this.pageURL;
   return this.pageURL.substring(0, ix);
}

js_HtmlPage_c.getPageVisitCount = js_Page_c.getPageVisitCount = function() {
   return this.pageVisitCount;
}

js_HtmlPage_c.setPageVisitCount = js_Page_c.setPageVisitCount = function(vc) {
   this.pageVisitCount = vc;
}

js_HtmlPage_c.getQueryParamProperties = js_Page_c.getQueryParamProperties = function() {
   return this.queryParamProperties;
}

js_HtmlPage_c.getPageJSFiles = function() {
   return this.pageJSFiles;
}

js_HtmlPage_c.setPageJSFiles = function(f) {
   this.pageJSFiles = f;
}

js_HtmlPage_c.getServerTagById = function(id) {
   for (var i = 0; i < sc$rootTagsArray.length; i++) {
      var rootTag = sc$rootTagsArray[i];
      var st = rootTag.serverTags[id];
      if (st)
         return st;
   }
   return null;
}

js_HtmlPage_c.onPageLoad = js_Page_c.onPageLoad = function() {
   sc_runRunLaterMethods(); // in case these trigger a global refresh
   if (js_Element_c.globalRefreshScheduled || this.refreshOnLoad)
      this.refresh();
   // If a global refresh is not needed, we'll just update the DOM
   else {
      this.updateDOM();
      if (!this.element)
         console.error("No element found for page"); // TODO: do a refresh here?
      sc_runClientInitJobs();
      this.refreshedOnce = true;
      this.refreshServerTags();
   }
}

js_HtmlPage_c.refresh = js_Page_c.refresh = function() {
   // Stop any child tag object refreshes since we are about to do the whole thing anyway
   js_Element_c.globalRefreshScheduled = true;

   // Do this right before we refresh.  That delays them till after the script code in the page
   // has been loaded so all of the dependencies are satisfied.
   sc_runRunLaterMethods();

   sc_checkRefresh();

   sc_runRunLaterMethods();
   // TODO: should we be refreshing only the body tag, not the head and HTML?
   /*
   if (this.body != null)
      this.body.refresh();
   else */
   js_HTMLElement_c.refresh.call(this);
   js_Element_c.globalRefreshScheduled = false;
   if (!this.refreshedOnce) {
      this.refreshedOnce = true;
      sc_runClientInitJobs();
   }
   this.refreshServerTags();
}

js_HtmlPage_c.schedURLUpdate = js_Page_c.schedURLUpdate = function() {
   if (!this.updateURLScheduled) {
      this.updateURLScheduled = true;
      sc_addRunLaterMethod(this, js_Page_c.updateURL, 5);
   }
}

js_HtmlPage_c.updateURL = js_Page_c.updateURL = function() {
   this.updateURLScheduled = false;
   if (this.urlChanged)
      this.updatePageFromURL(false);
   else
      this.updateURLFromProperties(false);

}

function sc_cleanURL(u) {
   var hix = u.indexOf("#");
   if (hix == u.length - 1)
      return u.substring(0,hix);
   return u;
}

function sc_sameURLs(u1,u2) {
   if (u1 === u2)
      return true;
   if (sc_cleanURL(u1) === sc_cleanURL(u2))
      return true;
   return false;
}

js_HtmlPage_c.updatePageFromURL = js_Page_c.updatePageFromURL = function(addListener) {
   var url = window.location.href;
   this.pageURL = url;
   this.urlChanged = false;
   var updateURLListener = addListener ? new js_RefreshURLListener(this) : null;
   var urlPropNames = [];
   var urlPropValues = {__pns:urlPropNames};
   if (this.urlParts != null) {
      this.processURLParams(window.location.pathname, this.urlParts, false, urlPropValues, updateURLListener);
   }
   if (this.queryParamProperties != null) {
      var qps = this.queryParamProperties;
      var qix = url.indexOf('?');
      var foundProps = {};
      if (qix != -1 && qix < url.length - 1) {
         var qstr = url.substring(qix+1);
         var qarr = qstr.split('&');
         for (var i = 0; i < qarr.length; i++) {
            var qent = qarr[i];
            var eix = qent.indexOf('=');
            if (eix != -1 && eix < qent.length) {
               var en = qent.substring(0, eix);
               var ev = decodeURIComponent(qent.substring(eix+1));
               for (var j = 0; j < qps.size(); j++) {
                  var qp = qps.get(j);
                  if (en.equals(qp.paramName)) {
                     qp.setPropertyValue(this, ev);
                     foundProps[qp.propName] = true;
                  }
               }
            }
         }
      }
      // Make sure any properties not in the URL get reset to null - this is required for resetting values
      // like when implementing the back button.
      for (var j = 0; j < qps.size(); j++) {
         var qp = qps.get(j);
         if (!foundProps[qp.propName])
            qp.setPropertyValue(this, null);
      }

      if (updateURLListener !== null) {
         for (var q = 0; q < qps.size(); q++) {
            var qp = qps.get(q);
            var pn = qp.propName;
            urlPropValues[pn] = sc_DynUtil_c.getPropertyValue(this, pn);
            urlPropNames.push(pn);
            sc_Bind_c.addListener(this, pn, updateURLListener, sc_IListener_c.VALUE_VALIDATED);
         }
      }
   }
   this.lastURLProps = urlPropValues;
   if (addListener && urlPropNames.length > 0) {
      var thisPage = this;
      history.replaceState(urlPropValues, "init page state");
      // Called every time local navigation changes both: a click on an a href="#" link or back/forward
      window.addEventListener('popstate', function(ev) {
         if (!sc_sameURLs(window.location.href, thisPage.pageURL)) {
            thisPage.urlChanged = true;
            thisPage.schedURLUpdate();
         }
         //thisPage.updatePageFromURL(false);
      });
   }
}

js_HtmlPage_c.processURLParams = js_Page_c.processURLParams = function(url, ups, opt, newURLProps, updateURLListener) {
   var urlNext = url;
   for (var i = 0; i < ups.size(); i++) {
      var up = ups.get(i);
      if (sc_instanceOf(up, String)) {
         if (urlNext.startsWith(up)) {
            urlNext = urlNext.substring(up.length);
         }
         else if (urlNext === "/" && up.startsWith(js_indexPattern))
            urlNext = urlNext.substring(js_indexPattern.length);
         else {
            var matched = false;
            // window.location.pathname might be the file system path for a client-only app - if so, 
            // need to skip the file system path name part in our URL parsing logic
            if (i === 0) {
               var path = "web" + up;
               var ix = urlNext.indexOf(path);
               if (ix !== -1) {
                  urlNext = urlNext.substring(ix+1);
                  matched = true;
               }
            }
            if (!matched) {
               if (!opt)
                  console.error("url: " + url + " expected to find: " + up + " but found: " + urlNext);
               break;
            }
         }
      }
      else if (up.parseletName) { // js_URLParamProperty
         var val;
         var ct = 0;
         if (urlNext.length === 0) {
            if (!opt)
               console.error("url: " + url + " does not match pattern");
            break;
         }
         if (up.parseletName === "urlString") {
            val = "";
            for (; ct < urlNext.length; ct++) {
               var c = urlNext.charAt(ct);
               if (c.match(/[-a-zA-Z0-9$_\+.!\*\(\),]/)) {
                  val += c;
               }
               else
                  break;
            }
         }
         else if (up.parseletName === "integerLiteral") {
            if (urlNext.charAt(0) === '-') {
               val = '-';
               ct++;
            }
            else
               val = "";
            for (; ct < urlNext.length; ct++) {
               var c = urlNext.charAt(ct);
               if (c.match(/[0-9]/)) {
                  val += c;
               }
               else
                  break;
            }
            val = parseInt(val);
         }
         else if (up.parseletName === "identifier") {
            val = "";
            var c = urlNext.charAt(0);
            if (c.match(/[a-zA-Z0-9_]/)) {
               val += c;
               ct++;
            }
            for (; ct < urlNext.length; ct++) {
               var c = urlNext.charAt(ct);
               if (c.match(/[a-zA-Z0-9_]/)) {
                  val += c;
               }
               else
                  break;
            }
         }
         else
            console.error("Unrecognized parselet type for url parameter: " + up.parseletName);
         up.setPropertyValue(this, val);
         urlNext = urlNext.substring(ct);

         // List for property changes and update the URL
         if (up.propName !== null && updateURLListener !== null) {
            var pn = up.propName;
            newURLProps[pn] = val;
            newURLProps.__pns.push(pn);
            sc_Bind_c.addListener(this, pn, updateURLListener, sc_IListener_c.VALUE_VALIDATED);
         }
      }
      else if (up.urlParts) { // js_OptionalURLParam
         if (urlNext.length === 0) {
            break;
         }
         urlNext = this.processURLParams(urlNext, up.urlParts, true, newURLProps, updateURLListener);
      }
   }
   return urlNext;
}

// Called to create, or update a server tag object, pointing to the DOM element specified by 'id'.
// It's called from a server response handler with an optional serverTag info object describing the properties the
// server is interested in. It returns the resulting tag object
js_Element_c.updateServerTag = function(tagObj, id, serverTag, addSync) {
   var element = document.getElementById(id);

   var isRepeat = js_RepeatServerTag_c.isRepeatId(id);

   if (element != null) {
      if (tagObj == null && element.scObj != null)
         tagObj = element.scObj;
      if (tagObj == null) {
         var tagClass = isRepeat ? js_RepeatServerTag_c : js_HTMLElement_c.tagNameToType[element.tagName.toLowerCase()];
         if (tagClass == null)
            tagClass = js_HTMLElement_c;
         if (tagClass != null) {
            tagObj = sc_DynUtil_c.createInstance(tagClass, null);
            tagObj.parentNode = this;
            tagObj.setId(id);
            tagObj.serverContent = true;
            tagObj.updateFromDOMElement(element);
            if (serverTag) {
               if (serverTag.initScript) {
                   tagObj.initScript = serverTag.initScript;
                   tagObj.runInitScript();
               }
               if (serverTag.stopScript)
                  tagObj.stopScript = serverTag.stopScript;
            }

            if (addSync) {
               if (isRepeat)
                  console.error("Synchronizing repeat tag - case not supported");

               var props = serverTag == null || serverTag.props == null ? tagObj.eventAttNames : serverTag.props.toArray().concat(tagObj.eventAttNames);

               sc_SyncManager_c.registerSyncInst(tagObj, id, true, false); // register but do not init because we do that next with a specific set of props

               // Synchronize the tag instance - this will add listeners for the properties we need to listen for the tag object.
               // The tag object will then listen for the appropriate DOM events and update the tag object as necessary.  That will
               // trigger queuing of sync events for these properties.
               sc_SyncManager_c.addSyncInst(tagObj, false, false, null, new sc_SyncProperties(null, null, props, 0), 0, 0);
            }
         }
      }
      // DOM element has changed
      else if (tagObj.element !== element) {
         if (js_Element_c.verbose)
            console.log("updating DOM element for tagObject with " + id);
         tagObj.updateFromDOMElement(element);
         // TODO: if the props in serverTag have changed we should update them here
      }
   }
   else {
      if (tagObj == null) {
         // When called from refreshServerTags, we don't want to consult the existing tag objects, or we won't find removed elements but we might receive 
         // events for an object which is being removed from the DOM.  So for resolveName we are having it return oldTags to avoid those errors.
         if (!addSync) 
            tagObj = js_HtmlPage_c.getServerTagById(id);
         if (tagObj != null) {
            return tagObj; // There was an old server tag by this name - we'll return it in case the DOM is being populated with this element
         }
         // no logging here - just return null since this is called for all sync lookups
      }
      else {
         if (js_Element_c.verbose)
            console.log("Removing serverTag object " + id + ": no longer in DOM");
         sc_DynUtil_c.dispose(tagObj);
         tagObj = null;
      }
   }
   return tagObj;
}

js_Element_c.scheduleRefresh = function() {
   sc_refresh();
}

// For readability in the logs, flexibility in code-gen and efficiency in rendering we send the start tag txt all at once so
// there's work here in parsing it and updating the DOM.
// NOTE: close replica in stags.js so keep these two in sync
js_Element_c.setStartTagTxt = function(startTagTxt) {
   var elem = this.element;
   if (elem != null) {
      if (!startTagTxt.startsWith("<")) {
         console.error("invalid start tag txt");
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
         console.error("Missing tag name");
         return;
      }
      if (!tagName.equalsIgnoreCase(elem.tagName)) {
         console.error("Invalid tag name change - current tag: " + elem.tagName + " != " + tagName);
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
               console.error("Invalid attribute");
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
                        console.error("Invalid attribute");
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
                  /*
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
                              // Just a backslash without escaping anything
                              avix--;
                              attVal += c;
                           }
                           continue;
                        }
                     }
                     else */ if (c == '&') {
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
               console.error("Unclosed string with: " + delim);
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
         console.error("Invalid start tag txt");
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
            if (newVal == true && elem.hasAttribute(newAttName))
               elem.removeAttribute(newAttName);
         }
         if (oldVal == null || !oldVal.equals(newVal)) {
            if (isInput && newAttName === "value") {
               elem.value = newVal;
            }
            elem.setAttribute(newAttName, newVal);
         }
         // Not sure why, but for some reason this is necessary for the innerHTML to be updated as is used in the test scripts for execServer - it may not be a great use case since we are updating the html of the DOM nodes individually
         else if (isInput && newAttName === "value")
            elem.setAttribute("value", newVal);
      }
      for (var i = 0; i < oldAtts.length; i++) {
         var oldAttName = oldAtts[i].name;
         if (!newAtts[oldAttName]) {
            elem.removeAttribute(oldAttName);
            i--;
         }
      }
   }
}

js_Element_c.setInnerHTML = function(htmlTxt) {
   if (this.element != null) {
      this.element.innerHTML = htmlTxt;
      js_HtmlPage_c.schedRefreshServerTags();
   }
}

js_HtmlPage_c.refreshServerTags = js_Page_c.refreshServerTags = function() {
   if (typeof sc_SyncManager_c !== "undefined") {
      // The sc.js.ServerTagManager object gets created by the sync layer but it's not synchronized itself.  Instead, just resolve it from the dyn system
      //var serverTagMgr = sc_DynUtil_c.resolveName("sc.js.ServerTagManager", false, false);
      var serverTagMgr = sc_SyncManager_c.getSyncInst("sc.js.PageServerTagManager");
      if (serverTagMgr != null) {
         var serverTags = this.serverTags;
         var newServerTags = serverTagMgr.newServerTags;
         if (newServerTags) {
            Object.assign(serverTags, newServerTags);
            serverTagMgr.newServerTags = null;
         }
         else if (!this.stInit) {
            newServerTags = serverTagMgr.serverTags;
            this.stInit = true;
         }
         if (newServerTags) {
            var it = newServerTags.entrySet().iterator();
            var id, serverTag;
            while (it.hasNext()) {
               var ent = it.next();
               id = ent.getKey();
               serverTag = ent.getValue();
               serverTags[id] = serverTag;
            }
         }
         var oldSyncState = sc_SyncManager_c.getSyncState();
         sc_SyncManager_c.setSyncState(sc_clInit(sc_SyncManager_SyncState_c).Initializing);
         try {
            var tagIds = Object.keys(serverTags);
            for (var i = 0; i < tagIds.length; i++) {
               id = tagIds[i];
               serverTag = serverTags[id];
               var tagObj = this.serverTagObjs[id];
               var newTagObj = js_HtmlPage_c.updateServerTag(tagObj, id, serverTag, true);
               if (newTagObj !== tagObj) {
                  if (newTagObj != null) {
                     this.serverTagObjs[id] = newTagObj;
                  }
                  else if (newTagObj === null) {
                     delete this.serverTagObjs[id];
                  }
               }
            }
            var rmTags = serverTagMgr.removedServerTags;
            if (rmTags) {
               for (var ri = 0; ri < rmTags.length; ri++) {
                  var rmId = rmTags[ri];
                  var rmSt = serverTags[rmId];
                  if (rmSt) {
                     rmTag = this.serverTagObjs[rmTag];
                     if (js_Element_c.verbose)
                        console.log("Removing serverTag object " + rmTag + ": removed from server");
                     if (rmTag.stopScript)
                        rmTag.runStopScript();
                     sc_DynUtil_c.dispose(rmTag);
                     delete serverTags[rmId];
                     delete this.serverTagObjs[rmId];
                  }
               }
               serverTagMgr.removedServerTags = null;
            }
         }
         finally {
            sc_SyncManager_c.setSyncState(oldSyncState);
         }
      }
   }
   js_HtmlPage_c.refreshServerTagsScheduled = false;
}

js_HtmlPage_c.refreshServerTagsScheduled = false;

js_HtmlPage_c.schedRefreshServerTags = function() {
   if (!js_HtmlPage_c.refreshServerTagsScheduled) {
      js_HtmlPage_c.refreshServerTagsScheduled = true;
      for (var i = 0; i < sc$rootTagsArray.length; i++) {
          var rootTag = sc$rootTagsArray[i];
          sc_addRunLaterMethod(rootTag, js_HtmlPage_c.refreshServerTags, 5);
      }
   }
}

function js_Img() {
   js_HTMLElement.apply(this, arguments);
   this.tagName = "img";
   this.src = null;
   this.height = this.width = 0;
}

js_Img_c = sc_newClass("sc.lang.html.Img", js_Img, js_HTMLElement, null);
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

function js_Style() {
   js_HTMLElement.apply(this, arguments);
   this.tagName = "style";
}

js_Style_c = sc_newClass("sc.lang.html.Style", js_Style, js_HTMLElement, null);

function js_StyleSheet() {
   js_Style.call(this);
   this.elementCreated = false;
   this.id = sc_CTypeUtil_c.getClassName(sc_DynUtil_c.getTypeName(this,false)) + ".css";
   this.tagName = null;
   // Schedule a refresh once this script is done loading
   sc_addLoadMethodListener(this, js_StyleSheet_c.refresh);
}

js_StyleSheet_c = sc_newClass("sc.lang.html.StyleSheet", js_StyleSheet, js_Style, null);

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

function js_RefreshURLListener(page) {
   sc_AbstractListener.call(this);
   this.page = page;
}

js_RefreshURLListener_c = sc_newClass("sc.lang.html.RefreshURLListener", js_RefreshURLListener, sc_AbstractListener, null);

// Called when a queryParam or URL param property has changed via the data binding events.  Schedule an update of the URL.
js_RefreshURLListener_c.valueValidated = function(obj, prop, detail, apply) {
   if (obj != this.page)
      console.error("Log error in refreshURLListener!");
   if (!sc_DynUtil_c.equalObjects(this.page.lastURLProps[prop], sc_DynUtil_c.getPropertyValue(this.page, prop))) {
      this.page.schedURLUpdate();
   }
}

function sc_replaceQueryString(url, newq) {
   var qix = url.indexOf('?');
   var post = "";
   if (qix == -1) {
      var hix = url.indexOf('#');
      if (hix != -1) {
         if (hix > url.length + 1)
            post = url.substring(hix);
         url = url.substring(0, hix);
      }
   }
   if (qix == -1)
      return url + newq + post;
   else
      return url.substring(0,qix) + newq;
}

js_HtmlPage_c.appendURLParts = js_Page_c.appendURLParts = function(baseURL, ups, opt) {
   var append = "";
   for (var i = 0; i < ups.size(); i++) {
      var up = ups.get(i);
      if (sc_instanceOf(up, String)) {
         append += up;
      }
      else if (up.parseletName) { // js_URLParamProperty
         if (up.propName !== null) {
            var propVal = sc_DynUtil_c.getPropertyValue(this, up.propName);
            if (propVal === null) {
               if (opt) // No value in this list of urlParts and we skip the entire thing - including any string contents since this is inside the []
                  return baseURL;
            }
            else {
               // TODO: look at parseletName for the conversion algorithm if this is not right
               append += propVal;
            }
         }
      }
      else if (up.urlParts) { // js_OptionalURLParam
         append += this.appendURLParts("", up.urlParts, true);
      }
   }
   return baseURL + append;
}

js_HtmlPage_c.updateURLFromProperties = js_Page_c.updateURLFromProperties = function() {
   var qps = this.queryParamProperties;
   var ups = this.urlParts;

   var newURLProps = {};
   var baseURL = location.href;
   var needsPush = false;
   if (ups !== null) {
      baseURL = this.appendURLParts("", ups, false);
      if (baseURL != location.href)
         needsPush = true;
   }
   if (qps !== null) {
      var qstr = null;
      for (var i = 0; i < qps.size(); i++) {
         var qp = qps.get(i);
         var propVal = sc_DynUtil_c.getPropertyValue(this, qp.propName);
         if (propVal) {
            if (qstr === null)
               qstr = "?";
            else
               qstr += "&";
            qstr += qp.paramName + "=" + propVal;
         }
         newURLProps[qp.propName] = propVal;
      }
      if (qstr !== null) {
         baseURL = sc_replaceQueryString(baseURL, qstr);
         needsPush = true;
      }
   }
   if (needsPush) {
      history.replaceState(newURLProps, "queryParamProps", baseURL);
      this.pageURL = window.location.href;
   }
   this.lastURLProps = newURLProps;
}

js_RefreshURLListener_c.toString = function() {
   return "<" + this.page.$protoName + " (url/query param listener)>";
}

function js_RefreshAttributeListener(scObj) {
   sc_AbstractListener.call(this);
   this.scObj = scObj;
}

js_RefreshAttributeListener_c = sc_newClass("sc.lang.html.RefreshAttributeListener", js_RefreshAttributeListener, sc_AbstractListener, null);

// Called when one of the tagObject properties we need to listen to has changed via the data binding events.  Update the corresponding DOM attribute
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
      js_HTMLElement_c.initDOMListener(domListener, prop, aliasName);
   }
   sc_addEventListener(elem, domListener.eventName, domListener.callback);
   if (this.scObj._eventListeners == null)
      this.scObj._eventListeners = [domListener];
   else
      this.scObj._eventListeners.push(domListener);
}

// Gets called when a new data binding expression is added to the tag object for a given property (e.g. a dom event property).
// Registers the corresponding DOM element listener so we can fire the appropriate change event on the tag object.
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

js_RepeatListener_c = sc_newClass("sc.lang.html.RepeatListener", js_RepeatListener, sc_AbstractListener, null);

js_RepeatListener_c.valueInvalidated = function(obj, prop, detail, apply) {
   var scObj = this.scObj;
   scObj.invalidateRepeatTags();
}

/*
js_RepeatListener_c.valueValidated = function(obj, prop, detail, apply) {
   var scObj = this.scObj;
   // When an update occurs to the repeat element, check if we need to refresh the list
   if (scObj.repeatTags === null || scObj.anyChangedRepeatTags()) {
      scObj.invalidateRepeatTags();
   }
}
*/

function js_IRepeatWrapper() {}

js_IRepeatWrapper_c = sc_newClass("sc.lang.html.IRepeatWrapper", js_IRepeatWrapper, null, null);

function js_Document(wrapped) {
   js_Document_c.documentWrapper = this;
   js_HTMLElement.call(this);
   this.setDOMElement(wrapped);
   this.activeElement = null;
}

js_Document_c = sc_newClass("sc.lang.html.Document", js_Document, js_HTMLElement, null);

js_Document_c.getDocument = function() {
   if (js_Document_c.documentWrapper === undefined) {
      js_Document_c.documentWrapper = new js_Document(document);
   }
   return js_Document_c.documentWrapper;
};

js_Document_c.setActiveElement = function(ae) {
   if (ae == this.activeElement)
      return;
   this.activeElement = ae;
   if (ae && ae.element)
      ae.element.focus();
   sc_Bind_c.sendChangedEvent(this, "activeElement");
};

js_Document_c.getActiveElement = function() {
   if (document.activeElement && document.activeElement.scObj) {
      this.activeElement = document.activeElement.scObj;
   }
   return this.activeElement;
};

function errorCountChanged() {
   sc_Bind_c.sendChangedEvent(js_Window_c.getWindow(), "errorCount");
}

function getThisPathname() {
   return this.pathname;
}

function js_Window() {
   js_Window_c.windowWrapper = this; // avoid recursive infinite loop
   this.document = document;
   this.location = window.location;
   this.location.getPathname = getThisPathname;
   this.location.setHref = function(href) {
      window.location.href = href;
   }
   this.location.getHref = function() {
      return window.location.href;
   }
   this.documentTag = js_Document_c.getDocument();
   window.sc_errorCountListener = errorCountChanged;
}

js_Window_c = sc_newClass("sc.lang.html.Window", js_Window, jv_Object, null);

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

js_Window_c.getDevicePixelRatio = function() {
   return window.devicePixelRatio;   
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

// Stores the meta-data for each page type
function js_PageInfo() {
   this.pageTypeName = null;
   this.pattern = null;
   this.pageType = null;
   this.queryParamProperties = null;
}

js_PageInfo_c = sc_newClass("sc.lang.html.PageInfo", js_PageInfo, jv_Object, null);

js_PageInfo_c.pages = {};

js_PageInfo_c.addPage = function(pageTypeName, pattern, pageType, queryParams, urlParts, constructorProps) {
   var pi = new js_PageInfo();
   pi.pageTypeName = pageTypeName;
   pi.pattern = pattern;
   pi.pageType = pageType;
   pi.queryParamProperties = queryParams;
   pi.urlParts = urlParts;
   pi.constructorProps = constructorProps;
   js_PageInfo_c.pages[pageTypeName] = pi;
}

function js_BaseURLParamProperty(enclType, propName, propType, req, constructor) {
   this.enclType = enclType;
   this.propName = propName;
   this.propType = propType;
   this.required = req;
   this.constructor = constructor;
}

js_BaseURLParamProperty_c = sc_newClass("sc.lang.html.BaseURLParamProperty", js_BaseURLParamProperty, jv_Object, null);

js_BaseURLParamProperty_c.setPropertyValue = function(pageInst, ev) {
   if (this.propType == Number_c) {
      if (ev == null || ev.length == 0)
         return; // if we have no value, just don't set the number
      ev = Number.parseInt(ev);
   }
   else if (this.propType != String_c) {
      console.error("No converter for query param property type: " + this.propName + ": " + this.propType);
      return;
   }
   sc_DynUtil_c.setPropertyValue(pageInst, this.propName, ev);
}

// Stores the meta-data for each page type
function js_QueryParamProperty(enclType, propName, paramName, propType, req, constructor) {
   if (arguments.length == 0) {
      enclType = propName = paramName = null;
      req = false;
   }
   js_BaseURLParamProperty.call(this, enclType, propName, propType, req, constructor);
   this.paramName = paramName;
}

// Stores the meta-data for each page type
function js_URLParamProperty(enclType, propName, propType, parseletName, req, constructor) {
   if (arguments.length == 0) {
      enclType = propName = null;
      req = false;
      parseletName = null;
   }
   js_BaseURLParamProperty.call(this, enclType, propName, propType, req, constructor);
   this.parseletName = parseletName;
}

js_URLParamProperty_c = sc_newClass("sc.lang.html.URLParamProperty", js_URLParamProperty, js_BaseURLParamProperty, null);

function js_OptionalURLParam(urlParts) {
   this.urlParts = urlParts;
}

function js_RepeatServerTag() {
   js_HTMLElement.call(this);
   this.serverRepeat = true;
}

// A class used to managed a 'repeat' tag which is rendered on the server.  The role of this class is to update
// the DOM when sync changes are received from the server with changes to the innerHTML property of this tag.
js_RepeatServerTag_c = sc_newClass("sc.lang.html.RepeatServerTag", js_RepeatServerTag, js_HTMLElement, null);

// For repeat server tags, no need to add any listeners
js_RepeatServerTag_c.updateFromDOMElement = function(newElement) {
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
            console.log("Warning: replacing repeat: " + sc_DynUtil_c.getInstanceId(newElement.scObj) + " with: " + sc_DynUtil_c.getInstanceId(this) + " for tag: " + this.tagName);
         }
         newElement.scObj = this;
      }
   }
   this.initState = 1;
}

js_RepeatServerTag_c.setInnerHTML = function(htmlTxt) {
   if (this.element != null) {
      do {
         var next = this.element.nextElementSibling;
         if (next == null) {
            console.error("Failed to find _Repeat_end tag for replacing repeat body");
            break;
         }
         if (js_RepeatServerTag_c.isRepeatEndId(next.id))
            break;
         this.element.parentNode.removeChild(next);
      } while (true);
      this.element.insertAdjacentHTML("afterend", htmlTxt);
      js_HtmlPage_c.schedRefreshServerTags();
   }
}

js_RepeatServerTag_c.isRepeatId = function(id) {
   return js_RepeatServerTag_c.getRepeatInnerName(id) != null;
}

js_RepeatServerTag_c.isRepeatId = function(id) {
   var ix = id.indexOf('_Repeat');
   if (ix === -1)
      return false;
   var eix = ix + '_Repeat'.length;
   if (eix === id.length)
      return true;
   return id.charAt(eix) === '_';
}

js_RepeatServerTag_c.isRepeatEndId = function(id) {
   var ix = id.indexOf('_Repeat')
   var ex = id.indexOf('_end');
   if (ix === -1 || ex === -1)
      return false;
   return true;
}

js_RepeatServerTag_c.setId = function(newVal) {
   this.id = newVal;
}

js_QueryParamProperty_c = sc_newClass("sc.lang.html.QueryParamProperty", js_QueryParamProperty, js_BaseURLParamProperty, null);

// Maps tag names to the base class object that implements those tag classes - defaults to js_HTMLElement_c for other tags
js_HTMLElement_c.tagNameToType = {input:js_Input_c, form:js_Form_c, select:js_Select_c, option:js_Option_c};

// Need to make these types using the sc java model for the sync system
js_Event_c = sc_newClass("sc.lang.html.Event", Event, null, null);
js_MouseEvent_c = sc_newClass("sc.lang.html.MouseEvent", MouseEvent, Event, null);

// Adding a default for this property which we use as a sync property to mirror currentTarget in the event
Event.prototype.currentTag = null;

if (typeof sc_SyncManager_c != "undefined") {
   sc_SyncManager_c.addSyncType(js_Event_c, null, ["type", "currentTag", "timeStamp"], null, sc_clInit(sc_SyncPropOptions_c).SYNC_INIT);
}

function sc_ServerTagManager() {   
   this.serverTags = null;
   this.newServerTags = null;
   this.removedServerTags = null;
   this.serverTagTypes = null;
   jv_Object.call(this);
}

var sc_ServerTagManager_c = sc_newClass("sc.js.ServerTagManager", sc_ServerTagManager, jv_Object, [sc_IObjectId]);

sc_ServerTagManager_c.setServerTags = function (sts)  {
   this.serverTags = sts;
};
sc_ServerTagManager_c.getServerTags = function ()  {
   return this.serverTags;
};
sc_ServerTagManager_c.setNewServerTags = function (sts)  {
   this.newServerTags = sts;
   js_HtmlPage_c.schedRefreshServerTags();
};
sc_ServerTagManager_c.getNewServerTags = function ()  {
   return this.newServerTags;
};
sc_ServerTagManager_c.setRemovedServerTags = function (sts)  {
   this.removedServerTags = sts;
   js_HtmlPage_c.schedRefreshServerTags();
};
sc_ServerTagManager_c.getRemovedServerTags = function ()  {
   return this.removedServerTags;
};
sc_ServerTagManager_c.setServerTagTypes = function (sts)  {
   this.serverTagTypes = sts;
};
sc_ServerTagManager_c.getServerTagTypes = function ()  {
   return this.serverTagTypes;
};
sc_ServerTagManager_c.getObjectId = function ()  {
   return "sc.js.PageServerTagManager";
};

function sc_ServerTag() {
   this.id = null;
   this.props = null;
   this.eventSource = false;
   this.marked = false;
   jv_Object.call(this);
}

var sc_ServerTag_c = sc_newClass("sc.js.ServerTag", sc_ServerTag, jv_Object, [sc_IObjectId]);

sc_ServerTag_c.equals = function (other)  {
   if (this.hasOwnProperty("$protoName")) {
      return jv_Class_c.equals.apply(this, arguments);
   }
   if ((other instanceof sc_ServerTag)) {
      var ot = (other);
      if (!this.id.equals(ot.id))
         return false;
      if (this === ot)
         return true;
      if (this.props !== ot.props && (this.props === null || ot.props === null))
         return false;
      if (this.eventSource !== ot.eventSource)
         return false;
      return true;
   }
   return false;
};
sc_ServerTag_c.toString = function ()  {
   if (this.hasOwnProperty("$protoName")) {
      return jv_Class_c.toString.apply(this, arguments);
   }
   return "id=" + this.id + "(" + this.props + ")";
};
sc_ServerTag_c.hashCode = function ()  {
   if (this.hasOwnProperty("$protoName")) {
      return jv_Class_c.hashCode.apply(this, arguments);
   }
   return this.id.hashCode();
};
sc_ServerTag_c.getObjectId = function ()  {
   return "sc.js.st_" + this.id;
};

function sc_URLPath(u, n, k, p, rt) {
   this.url = u;
   this.name = n;
   this.keyName = k;
   this.pageType = p;
   this.realTime = rt;
}

sc_URLPath_c = sc_newClass("sc.js.URLPath", sc_URLPath, jv_Object, [sc_IObjectId]);

Event.prototype.hashCode = jv_Object_c.hashCode;
Event.prototype.equals = jv_Object_c.equals;
Event.prototype.getClass = jv_Object_c.getClass;

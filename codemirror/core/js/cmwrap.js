
function sc_CodeMirror(id, listener) {
   this.id = id;
   this.options = { lineNumbers: true,
                    mode: "text/x-java",
                    matchBrackets: true,
                    keyMap: "vim",
                    hintOptions: {hint: sc_CodeMirror_c.getHints},
                    extraKeys: {"Ctrl-Space":"autocomplete"} };
   //this.options = JSON.parse(optStr);
   this.cm = null;
   this.textarea = null;
   this.mode = null;
   this.listener = listener;
   this.errors = [];

/*
   CodeMirror.registerHelper("hint", "text/x-java", sc_CodeMirror_c.getHints);
   CodeMirror.registerHelper("hint", "text/x-stratacode", sc_CodeMirror_c.getHints);
   CodeMirror.registerHelper("hint", "text/x-jsp", sc_CodeMirror_c.getHints);
*/
}

var sc_CodeMirror_c = sc_newClass("sc.codemirror.CodeMirror", sc_CodeMirror, jv_Object, null);

sc_CodeMirror_c.getHints = function(cm, callback, options) {
   var cur = cm.getCursor();
   var thisWrapper = cm.scWrapper;
   var token = cm.getTokenAt(cur);
   var str = token.string;
   var res;
   if (str.trim().length == 0) {
      res = cm.getHelper(cm.getCursor(), "hintWords");
      callback.call(null, { list:res, from: cur, to: cur});
   }
   else {
      var cursorPos = cm.indexFromPos(cur);
      var tlen = str.length;
      var prePos = cm.posFromIndex(cursorPos-tlen);

      var remResult = thisWrapper.listener.getSuggestionsForPos(cursorPos);
      remResult.responseListener = new sc_IResponseListener();
      remResult.responseListener.response = function(resultList) {
         var arr = resultList.toArray();
         callback.call(null, {list:arr, from: prePos, to: cur});
      };
      remResult.responseListener.error = function(ec,e) {
         console.error("Error retrieving suggestions: " + e);
      };
   }
}

sc_CodeMirror_c.getHints.async = true;

sc_CodeMirror_c.createFromTextArea = function(id, optStr, listener) {
   var cm = new sc_CodeMirror(id, optStr, listener);
   cm.init();
   return cm;
}

sc_CodeMirror_c.init = function() {
   this.textarea = document.getElementById(this.id);
   if (this.textarea) {
      this.cm = CodeMirror.fromTextArea(this.textarea, this.options);

      var thisWrapper = this;
      this.cm.scWrapper = this;

      this.cm.on("change", function(cm, changedObj) {
         // Skip changes due to us changing the value
         if (changedObj.origin != "setValue")
            thisWrapper.listener.contentChanged();
      });
      this.cm.on("cursorActivity", function(cm) {
         thisWrapper.listener.cursorChanged();
      });
   }
}

sc_CodeMirror_c.modesPerExt = {scj:'text/x-java', java:'text/x-java', sc:'text/x-stratacode', schtml:'application/x-jsp'};

sc_CodeMirror_c.updateContent = function(text, fileName, cursorIndex) {
  var newTA = document.getElementById(this.id);
  var extIx = fileName.lastIndexOf('.');
  if (extIx != -1 && extIx != fileName.length - 1) {
     var ext = fileName.substring(extIx+1);
     var mode = sc_CodeMirror_c.modesPerExt[ext];
     if (!mode)
        console.error("No CodeMirror mode for file: " + fileName);
     else
        this.setMode(mode);
  }
  if (newTA != this.textarea) {
     if (this.cm) {
       this.removeCodeMirror();
     }
     this.textarea = newTA;
  }
  if (!this.cm && this.textarea) {
     this.init();
  }
  if (!text)
     text = "";
  if (this.cm) {
     this.cm.getDoc().setValue(text);
     this.reapplyErrors();
     if (cursorIndex != -1)
        this.setCursorIndex(cursorIndex)

     this.cm.focus();
  }
}

sc_CodeMirror_c.getContent = function() {
   if (this.cm) {
      return this.cm.getValue();
   }
   return null;
}

sc_CodeMirror_c.getCursorIndex = function() {
   if (this.cm) {
      // cm.getCursor() returns {line,ch} whereas we use an index offset in the editor context
      return this.cm.indexFromPos(this.cm.getCursor());
   }
   return 0;
}

sc_CodeMirror_c.setCursorIndex = function(ind) {
   if (this.cm) {
      this.cm.setCursor(this.cm.posFromIndex(ind));
   }
}

sc_CodeMirror_c.setMode = function(mode) {
   if (this.mode != mode) {
      if (this.cm) {
         // Seem to have problems changing mode with an existing cm
         // so remove the old one before creating a new one on the same element
         this.removeCodeMirror();
      }
      this.options.mode = mode;
      this.mode = mode;
   }
}

sc_CodeMirror_c.setOption = function(name,val) {
   this.options[name] = val;
   if (this.cm)
      this.cm.setOption(name, val);
}

sc_CodeMirror_c.addError = function(err,six,eix,nf) {
   var textMarker = null;
   if (eix == six)
      six = six - 10;
   eix++;

   if (this.cm) {
      var cssClass = nf ? 'notFound' : 'syntaxError';
      textMarker = this.cm.markText(this.cm.posFromIndex(six), this.cm.posFromIndex(eix), {className: cssClass, title: err});
   }
   this.errors.push({textMarker:textMarker, err:err, six:six, eix:eix, nf:nf});
}

sc_CodeMirror_c.reapplyErrors = function() {
   for (var i = 0; i < this.errors.length; i++) {
      var error = this.errors[i];
      var cssClass = error.nf ? 'notFound' : 'syntaxError';
      error.textMarker = this.cm.markText(this.cm.posFromIndex(error.six), this.cm.posFromIndex(error.eix), {className: cssClass, title: error.err});
   }
   this.cm.refresh();
}

sc_CodeMirror_c.clearErrors = function() {
   for (var i = 0; i < this.errors.length; i++) {
      var error = this.errors[i];
      if (error.textMarker)
         error.textMarker.clear();
   }
   this.errors = [];
}

sc_CodeMirror_c.refresh = function() {
   if (this.cm != null)
      this.cm.refresh();
}

sc_CodeMirror_c.removeCodeMirror = function() {
   // Undo the affects of fromTextArea - (see code in toTextArea for reference)
   this.textarea.parentNode.removeChild(this.cm.getWrapperElement());
   this.cm = null;
}

function sc_CodeMirror_IEditorEventListener() {}

var sc_CodeMirror_IEditorEventListener_c = sc_newInnerClass("sc.codemirror.CodeMirror.IEditorEventListener", sc_CodeMirror_IEditorEventListener, sc_CodeMirror, jv_Object, null);


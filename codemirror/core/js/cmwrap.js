
function sc_CodeMirror(id, optStr) {
   this.id = id;
   this.options = JSON.parse(optStr);
   this.cm = null;
   this.textarea = null;
   this.mode = null;
   this.changedCount = 0;
}

sc_CodeMirror_c = sc_newClass("sc.codemirror.CodeMirror", sc_CodeMirror, jv_Object, null);

sc_CodeMirror_c.createFromTextArea = function(id, optStr) {
   var cm = new sc_CodeMirror(id, optStr);
   cm.init();
   return cm;
}

sc_CodeMirror_c.init = function() {
   this.textarea = document.getElementById(this.id);
   if (this.textarea) {
      this.cm = CodeMirror.fromTextArea(this.textarea, this.options);

      var thisWrapper = this;

      this.cm.on("change", function(cm, changedObj) {
         thisWrapper.setChangedCount(thisWrapper.changedCount+1);
      });
   }
}

sc_CodeMirror_c.modesPerExt = {scj:'text/x-java', java:'text/x-java', sc:'text/x-stratacode', schtml:'application/x-jsp'};

sc_CodeMirror_c.updateContent = function(text, fileName) {
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
  if (this.cm)
     this.cm.getDoc().setValue(text);
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

sc_CodeMirror_c.setChangedCount = function(ct) {
   this.changedCount = ct;
   sc_Bind_c.sendChange(this, "changedCount", ct);
}

sc_CodeMirror_c.getChangedCount = function() {
   return this.changedCount;
}

sc_CodeMirror_c.removeCodeMirror = function() {
   // Undo the affects of fromTextArea - (see code in toTextArea for reference)
   this.textarea.parentNode.removeChild(this.cm.getWrapperElement());
   this.cm = null;
}

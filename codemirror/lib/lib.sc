// To install: npm install codemirror
package sc.html.tag;


codemirror.lib extends html.schtml {
  String codeMirrorPath = "/Users/jvroom/node_modules/codemirror";

  hidden = true;
  compiledOnly = true;
  codeType = CodeType.Framework;

  void init() {
     addSrcPath(FileUtil.concat(codeMirrorPath, "lib"), "cm-lib", "web/cm/lib", "web");
     addSrcPath(FileUtil.concat(codeMirrorPath, "keymap"), "cm-keymap", "web/cm/keymap", "web");
     addSrcPath(FileUtil.concat(codeMirrorPath, "mode"), "cm-mode", "web/cm/mode", "web");
     addSrcPath(FileUtil.concat(codeMirrorPath, "addon"), "cm-addon", "web/cm/addon", "web");
  }
}

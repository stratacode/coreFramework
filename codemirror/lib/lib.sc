// To install: npm install codemirror
// TODO: the patches should no longer be needed - remove this comment
// after testing that it's true
//   - NOTE: must apply a few patches from /jjv/codemirror git repo: 
//  ${codeMirrorPath}/mode/htmlmixed/htmlmixed.js, plus adding the
//  sc syntax type and a change to the default css styling for height: 100%;
package sc.html;


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

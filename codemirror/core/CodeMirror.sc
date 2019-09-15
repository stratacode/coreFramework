// This is the Java stub for the JS implementation in js/cmwrap.js. It may
// actually run on the server but it doesn't have to do anything there
// but avoid a null pointer exception by returning a valid value.
@sc.js.JSSettings(jsLibFiles="js/cmwrap.js", prefixAlias="sc_")
class CodeMirror {

  @sc.bind.Bindable
  int changedCount;

  static CodeMirror createFromTextArea(String textAreaId, String options) {
     return new CodeMirror();
  }

  void updateContent(String text, String fileName) {
  }

  void setOption(String name, String value) {
  }
}

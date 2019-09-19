// This is the Java stub for the JS implementation in js/cmwrap.js. It may
// actually run on the server but it doesn't have to do anything there
// but avoid a null pointer exception by returning a valid value.
@sc.js.JSSettings(jsLibFiles="js/cmwrap.js", prefixAlias="sc_")
class CodeMirror {

   interface IEditorEventListener {
      void contentChanged();
   }

   static CodeMirror createFromTextArea(String textAreaId, String options, IEditorEventListener listener) {
      return new CodeMirror();
   }

   void updateContent(String text, String fileName) {
   }

   void setOption(String name, String value) {
   }

   String getContent() {
      return null;
   }

   void setCursorIndex(int ind) {
   }

   int getCursorIndex() {
      return -1;
   }

   void clearErrors() {
   }

   void addError(String msg, int startIx, int endIx, boolean notFound) {
   }

   void refresh() {
   }
}

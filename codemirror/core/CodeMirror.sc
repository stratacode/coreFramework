import sc.dyn.RemoteResult;
// For the JS version, this class is replaced by the native code in cmwrap.js.

// TODO: The server only version is not yet implemented. I think we can use DynUtil.evalScript invoke the code in cmwrap.js and
// it would not be very difficult. Basically create a wrapper on the remote side and just call methods on it. We also need a way
// to know we are in server only mode or at least make sure the code here works in sync with the client version.
@sc.js.JSSettings(jsLibFiles="js/cmwrap.js", prefixAlias="sc_")
class CodeMirror {

   interface IEditorEventListener {
      void contentChanged();
      void cursorChanged();
      // Begins a remote request to retrieve the suggestions for the given cursor position
      RemoteResult getSuggestionsForPos(int cursorPos);
   }

   static CodeMirror createFromTextArea(String textAreaId, IEditorEventListener listener) {
      return new CodeMirror();
   }

   void updateContent(String text, String fileName, int cursorIndex) {
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

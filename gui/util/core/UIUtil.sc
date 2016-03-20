// Utilities you can use that are portable across UI frameworks
class UIUtil {
   static abstract void showErrorDialog(Object root, String message, String title);

   static abstract int showOptionDialog(Object root, String message, String title, Object[] options, Object defaultTitle);
}

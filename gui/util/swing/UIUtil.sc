class UIUtil {
   static void showErrorDialog(Object root, String message, String title) {
      JOptionPane.showMessageDialog((java.awt.Component) root, message, title, JOptionPane.ERROR_MESSAGE);
   }

   static int showOptionDialog(Object root, String message, String title, Object[] options, Object defaultTitle) {
      return JOptionPane.showOptionDialog((java.awt.Component) root, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
          options,  //the titles of buttons
          defaultTitle); //default button title
   }
}

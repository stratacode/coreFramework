//@MainInit
class TestTextArea extends AppFrame {
   object textScrollPane extends RTextScrollPane {
      viewportView = textArea;
      size := SwingUtil.dimension(windowWidth, windowHeight);
      object textArea extends RSyntaxTextArea {
         syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_JAVA;
         antiAliasingEnabled = true;
         text = "test";
      }
   }
}

package sc.rtextarea;

import sc.rtextarea.RTextScrollPane;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import sc.rtextarea.RSyntaxTextArea;

public swing.rtextarea extends swing.core {
   codeType = sc.layer.CodeType.Framework;
   codeFunction = sc.layer.CodeFunction.UI;

   classPath=sc.util.FileUtil.listFiles(getRelativeFile("./lib"),".*\\.jar");
   compiledOnly = true;
}

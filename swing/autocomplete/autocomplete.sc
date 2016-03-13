package sc.swing;

import sc.swing.SCCompletionProvider;
import org.fife.ui.autocomplete.AutoCompletion;


swing.autocomplete extends swing.rtextarea {
   codeType = sc.layer.CodeType.Framework;
   codeFunction = sc.layer.CodeFunction.UI;

   classPath=sc.util.FileUtil.listFiles(getRelativeFile("./lib"),".*\\.jar");
   compiledOnly = true;
}

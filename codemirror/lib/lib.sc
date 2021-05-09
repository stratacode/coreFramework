// To install: npm install codemirror
//   - NOTE: must apply a few patches from a version of code mirror from 9/19
//  ${codeMirrorPath}/mode/htmlmixed/htmlmixed.js, plus adding the
//  sc syntax type and a change to the default css styling for height: 100%;
package sc.html;


codemirror.lib extends html.schtml {
   String codeMirrorPath; 

   hidden = true;
   compiledOnly = true;
   codeType = CodeType.Framework;

   object codeMirrorPackage extends RepositoryPackage {
      packageName = "codeMirror";
      type = "url";
      url = "https://www.stratacode.com/packages/CodeMirror-patched-9-19.jar";
      unzip = true;
      unwrapZip = false; // The zip directory is already wrapped
   }

   void start() {
      if (!codeMirrorPackage.installed || codeMirrorPackage.installedRoot == null) {
         System.err.println("*** Warning code mirror package not installed!");
      }

         String codeMirrorPath = FileUtil.concat(codeMirrorPackage.installedRoot, "CodeMirror");
         if (!new File(codeMirrorPath).isDirectory())
            System.err.println("*** Warning code mirror package not found at: " + codeMirrorPath);
         else {
            addSrcPath(FileUtil.concat(codeMirrorPath, "lib"), "cm-lib", "web/cm/lib", "web");
            addSrcPath(FileUtil.concat(codeMirrorPath, "keymap"), "cm-keymap", "web/cm/keymap", "web");
            addSrcPath(FileUtil.concat(codeMirrorPath, "mode"), "cm-mode", "web/cm/mode", "web");
            addSrcPath(FileUtil.concat(codeMirrorPath, "addon"), "cm-addon", "web/cm/addon", "web");
         }
   }
}

import sc.parser.Language;
import sc.parser.ParseError;
import sc.parser.ParseUtil;
import sc.lang.JavaLanguage;
import sc.lang.js.JSLanguage;
import sc.lang.SCLanguage;
import sc.lang.java.JavaModel;
import sc.lang.java.BodyTypeDeclaration;
import sc.lang.js.JSRuntimeProcessor;
import sc.lang.js.JSRuntimeProcessor.JSFileEntry;
import sc.lang.js.JSRuntimeProcessor.JSFileBodyCache;

import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;

import sc.parser.IParseNode;
import sc.layer.LayeredSystem;
import sc.layer.Options;
import sc.layer.Layer;
import sc.layer.SrcEntry;
import sc.lang.java.TypeDeclaration;

import sc.util.FileUtil;

object CvtForm {
   CvtManager mgr = CvtManager;

   String inputSrcText;
   String inputErrors;
   String resultSrcText;

   List<String> jsFiles;

   String jsFilePrefix;

   void doConvert(int cvtIx) {
      inputErrors = null;
      CvtManager.CvtImpl cvtImpl = mgr.convertImpls[cvtIx];
      if (inputSrcText == null || inputSrcText.length() == 0) {
         inputErrors = "No input source code";
      }
      Object parseRes = cvtImpl.fromLang.getInputLanguage().parseString(inputSrcText);
      if (parseRes instanceof ParseError) {
         ParseError parseError = (ParseError) parseRes;
         inputErrors = "Parse error: " + parseError.errorStringWithLineNumbers(inputSrcText);
         return;
      }

      Object semNode = ParseUtil.nodeToSemanticValue(parseRes);

      if (!(semNode instanceof JavaModel)) {
         inputErrors = "Input language: " + cvtImpl.fromLang.getInputLanguage() + " returned invalid type: " + parseRes;
         return;
      }

      LayeredSystem cvtSys = cvtImpl.sys;
      JavaModel inputModel = (JavaModel) semNode;

      jsFilePrefix = cvtImpl.jsPrefix;

      String absFileName = null;
      try {
         cvtSys.acquireDynLock(false);
         if (cvtSys.runtimeProcessor != null)
            cvtSys.runtimeProcessor.initRuntime(true);
         inputModel.setLayeredSystem(cvtSys);

         TypeDeclaration modelType = inputModel.getModelTypeDeclaration();
         if (modelType == null || modelType.getTypeName() == null) {
            inputErrors = "No top-level type name found in code";
            return;
         }
         String pkgName = inputModel.getPackagePrefix();
         Layer cvtLayer = cvtSys.buildLayer;
         String pkgPath = pkgName == null ? null : pkgName.replace(".", FileUtil.FILE_SEPARATOR);
         String relFileName = FileUtil.addExtension(FileUtil.concat(pkgPath, modelType.typeName),
                                                    cvtImpl.fromLang.getInputLanguage().defaultExtension);
         String projectDir = cvtLayer.layerPathName;
         absFileName = FileUtil.concat(projectDir, relFileName);
         // TODO: do we temporarily save the file here? or split the code out so that users upload files into a user-specific
         // directory that is temporarily added to the srcPath of the buildLayer, then remove it from the srcPath (along with
         // all of the models)
         SrcEntry srcEnt = new SrcEntry(cvtLayer, absFileName, relFileName);
         FileUtil.saveStringAsFile(srcEnt.absFileName, inputSrcText, true);
         inputModel.setSrcFile(srcEnt);
         inputModel.layer = cvtLayer;
         cvtSys.buildingSystem = true;

         JSRuntimeProcessor jsProc = null;
         String jsFile = null;
         JSFileBodyCache jsFileBodyCache = null;
         if (cvtSys.runtimeProcessor instanceof JSRuntimeProcessor) {
            jsProc = (JSRuntimeProcessor) cvtSys.runtimeProcessor;
            jsFile = "CvtForm.js";
            jsFileBodyCache = jsProc.createJSFile(jsFile);
            jsProc.addJSFile(jsFile);
            jsProc.addJsModuleName(modelType.getFullTypeName(), jsFile);
         }

         ParseUtil.initAndStartComponent(inputModel);
         if (inputModel.hasErrors()) {
            inputErrors = inputModel.getErrorMessagesAsString();
         }

         if (cvtSys.runtimeProcessor != null)
            cvtSys.runtimeProcessor.postStart(cvtSys, cvtLayer);

         inputModel.process();

         List<SrcEntry> xformFiles = cvtSys.getProcessedFiles(inputModel, cvtLayer, true);
         int numXformFiles = xformFiles.size();
         if (xformFiles == null || numXformFiles == 0) {
            inputErrors = "Conversion failed with an internal error";
            return;
         }

         if (jsProc != null) {
            LinkedHashMap<JSFileEntry,Boolean> typesInFile = new LinkedHashMap<JSFileEntry,Boolean>();
            jsProc.addTypeToFileWithDeps(modelType, typesInFile, jsFile, cvtLayer);
            resultSrcText = jsFileBodyCache.jsFileBody.toString();
         }
         else
            resultSrcText = FileUtil.getFileAsString(xformFiles.get(numXformFiles-1).absFileName);

         jsFiles = cvtSys.getCompiledFiles("js", inputModel.getModelTypeName());

         inputErrors = "";
      }
      finally {
         if (cvtSys.runtimeProcessor != null)
            cvtSys.runtimeProcessor.clearRuntime();
         cvtSys.resetBuild(true);
         cvtSys.buildingSystem = false;

         cvtSys.releaseDynLock(false);

         if (absFileName != null) {
            new java.io.File(absFileName).delete();
         }
      }
   }
}
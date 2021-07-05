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

import java.io.File;

import sc.util.ArrayList;

import sc.parser.IParseNode;
import sc.layer.LayeredSystem;
import sc.layer.Options;
import sc.layer.Layer;
import sc.layer.SrcEntry;
import sc.lang.java.TypeDeclaration;

import sc.util.FileUtil;

@Component
class CvtForm {
   CvtManager mgr = CvtManager;

   String inputSrcText;
   String resultSrcText;
   String statusMessage;

   List<String> jsFiles;

   String jsFilePrefix;

   // Needs to be session scoped so that it's shared with the CvtUploadPage that adds the modules on file upload
   CvtUploadModel cvtUploadModel = CvtUploadModel;

   boolean uploadVisible = true;

   boolean useModules = true;

   boolean showTerms = true;

   void init() {
      cvtUploadModel.cvtForm = this;
   }

   JavaModel doUpload() {
      cvtUploadModel.inputErrors = null;
      CvtManager.CvtImpl cvtImpl = mgr.convertImpls[getConverterIndex()];
      if (inputSrcText == null || inputSrcText.length() == 0) {
         cvtUploadModel.inputErrors = "No input source code";
         resultSrcText = null;
         jsFiles = null;
         return null;
      }
      Object parseRes = cvtImpl.fromLang.getInputLanguage().parseString(inputSrcText);
      if (parseRes instanceof ParseError) {
         ParseError parseError = (ParseError) parseRes;
         cvtUploadModel.inputErrors = "Parse error: " + parseError.errorStringWithLineNumbers(inputSrcText);
         resultSrcText = null;
         jsFiles = null;
         return null;
      }

      Object semNode = ParseUtil.nodeToSemanticValue(parseRes);

      if (!(semNode instanceof JavaModel)) {
         cvtUploadModel.inputErrors = "Input language: " + cvtImpl.fromLang.getInputLanguage() + " returned invalid type: " + parseRes;
         resultSrcText = null;
         jsFiles = null;
         return null;
      }
      JavaModel inputModel = (JavaModel) semNode;

      if (inputModel.getModelTypeDeclaration() != null) {
         if (cvtUploadModel.uploadedModels == null) {
            cvtUploadModel.uploadedModels = new ArrayList<JavaModel>();
         }
         cvtUploadModel.uploadedModels.add(inputModel);

         statusMessage = "Uploaded Java class: " + inputModel.getModelTypeDeclaration().getFullTypeName();
      }
      else
         statusMessage = "No class defined in file";

      inputSrcText = null;

      return inputModel;
   }

   int getConverterIndex() {
      return useModules ? 0 : 1;
   }

   void doConvert() {
      if (inputSrcText != null && inputSrcText.length() > 0)
          doUpload();

      LayeredSystem mainSys = LayeredSystem.getCurrent();

      CvtManager.CvtImpl cvtImpl = mgr.convertImpls[getConverterIndex()];
      if (cvtUploadModel.uploadedModels == null || cvtUploadModel.uploadedModels.size() == 0) {
         cvtUploadModel.inputErrors = "No files have been uploaded";
         resultSrcText = null;
         jsFiles = null;
         return;
      }

      LayeredSystem cvtSys = cvtImpl.sys;

      jsFilePrefix = cvtImpl.jsPrefix;
      JSRuntimeProcessor jsProc = null;
      if (cvtSys.runtimeProcessor instanceof JSRuntimeProcessor) {
         jsProc = (JSRuntimeProcessor) cvtSys.runtimeProcessor;
      }

      List<String> filesToRemove = new ArrayList<String>();

      String absFileName = null;
      cvtUploadModel.inputErrors = null;
      boolean completed = false;
      try {
         cvtSys.acquireDynLock(false);
         if (cvtSys.runtimeProcessor != null)
            cvtSys.runtimeProcessor.initRuntime(true);

         cvtSys.buildingSystem = true;
         Layer cvtLayer = cvtSys.buildLayer;
         String jsFile = "CvtForm.js";
         JSFileBodyCache jsFileBodyCache = null;
         if (jsProc != null)
            jsFileBodyCache = jsProc.createJSFile(jsFile);

         for (JavaModel inputModel:cvtUploadModel.uploadedModels) {
            inputModel.setLayeredSystem(cvtSys);

            TypeDeclaration modelType = inputModel.getModelTypeDeclaration();
            if (modelType == null || modelType.getTypeName() == null) {
               cvtUploadModel.inputErrors = "No top-level type name found in code";
               return;
            }
            String pkgName = inputModel.getPackagePrefix();
            String pkgPath = pkgName == null ? null : pkgName.replace(".", FileUtil.FILE_SEPARATOR);
            String relFileName = FileUtil.addExtension(FileUtil.concat(pkgPath, modelType.typeName),
                                                       cvtImpl.fromLang.getInputLanguage().defaultExtension);
            String projectDir = cvtLayer.layerPathName;
            absFileName = FileUtil.concat(projectDir, relFileName);
            // TODO: do we temporarily save the file here? or split the code out so that users upload files into a user-specific
            // directory that is temporarily added to the srcPath of the buildLayer, then remove it from the srcPath (along with
            // all of the models)
            SrcEntry srcEnt = new SrcEntry(cvtLayer, absFileName, relFileName);
            FileUtil.saveStringAsFile(srcEnt.absFileName, inputModel.parseNode.toString(), true);
            inputModel.setSrcFile(srcEnt);
            inputModel.layer = cvtLayer;

            cvtSys.addNewModel(inputModel, cvtLayer, null, null, false, false);

            if (cvtSys.runtimeProcessor instanceof JSRuntimeProcessor) {
               jsProc = (JSRuntimeProcessor) cvtSys.runtimeProcessor;
               jsProc.addJSFile(jsFile);
               jsProc.addJsModuleName(modelType.getFullTypeName(), jsFile);
            }
         }

         int numErrors = 0;

         for (JavaModel inputModel:cvtUploadModel.uploadedModels) {
            ParseUtil.initAndStartComponent(inputModel);
            if (inputModel.hasErrors()) {
               if (cvtUploadModel.inputErrors == null)
                  cvtUploadModel.inputErrors = "";
               cvtUploadModel.inputErrors += "- Errors for type: " + inputModel.getModelTypeName() + ":\n"
                               +  inputModel.getErrorMessagesAsString() + "\n---\n";

               logError(cvtUploadModel.inputErrors);
               numErrors++;
            }
         }

         if (cvtSys.runtimeProcessor != null)
            cvtSys.runtimeProcessor.postStart(cvtSys, cvtLayer);

         for (JavaModel inputModel:cvtUploadModel.uploadedModels) {
            inputModel.process();
         }

         resultSrcText = "";

         jsFiles = new ArrayList<String>();

         int numLines = 0;
         int numFiles = 0;

         for (JavaModel inputModel:cvtUploadModel.uploadedModels) {
            TypeDeclaration modelType = inputModel.getModelTypeDeclaration();
            List<SrcEntry> xformFiles = cvtSys.getProcessedFiles(inputModel, cvtLayer, true);
            int numXformFiles = xformFiles.size();
            if (xformFiles == null || numXformFiles == 0) {
               if (cvtUploadModel.inputErrors == null)
                  cvtUploadModel.inputErrors = "";
               cvtUploadModel.inputErrors += "Conversion failed with an internal error\n";
               return;
            }

            numLines += FileUtil.countLinesInFile(new File(inputModel.getSrcFile().absFileName));
            numFiles++;

            for (SrcEntry xformSrcEnt:xformFiles) {
               filesToRemove.add(xformSrcEnt.absFileName);
            }

            if (jsProc != null) {
               LinkedHashMap<JSFileEntry,Boolean> typesInFile = new LinkedHashMap<JSFileEntry,Boolean>();
               jsProc.addTypeToFileWithDeps(modelType, typesInFile, jsFile, cvtLayer);
            }
            else
               resultSrcText += FileUtil.getFileAsString(xformFiles.get(numXformFiles-1).absFileName);

            ArrayList<String> newJSDepFiles = new ArrayList<String>(cvtSys.getCompiledFiles("js", inputModel.getModelTypeName()));
            for (String newJsDepFile:newJSDepFiles) {
               if (!jsFiles.contains(newJsDepFile))
                  jsFiles.add(newJsDepFile);
            }
         }

         if (jsProc != null)
            resultSrcText = jsFileBodyCache.jsFileBody.toString();
         jsFiles.remove(jsFile); // we display this text in resultSrcText so no need for the dependent file

         uploadVisible = false;
         completed = true;
         sendConvertEvent(numFiles, numLines, numErrors, useModules);
      }
      catch (RuntimeException exc) {
         logError("Runtime exception converting code: " + exc.toString());
      }
      finally {
         if (cvtUploadModel.uploadedModels != null) {
            for (JavaModel inputModel:cvtUploadModel.uploadedModels) {
               if (inputModel.layer != null && inputModel.started && inputModel.getSrcFile() != null) {
                  cvtSys.removeModel(inputModel, true);
                  inputModel.stop();
               }
            }
         }
         if (cvtSys.runtimeProcessor != null)
            cvtSys.runtimeProcessor.clearRuntime();
         cvtSys.resetBuild(true);
         cvtSys.buildingSystem = false;

         LayeredSystem.setCurrent(mainSys);

         cvtSys.releaseDynLock(false);

         if (cvtUploadModel.uploadedModels != null) {
            for (JavaModel inputModel:cvtUploadModel.uploadedModels) {
               SrcEntry srcEnt = inputModel.getSrcFile();
               if (srcEnt != null) {
                  sc.layer.LayerUtil.removeFileAndClasses(srcEnt.absFileName);
               }
            }
         }
         for (String fileToRemove:filesToRemove) {
            sc.layer.LayerUtil.removeFileAndClasses(fileToRemove);
         }
      }
   }

   void doClear() {
      cvtUploadModel.uploadedModels = null;
      doUploadReset();
   }

   void doUploadReset() {
      uploadVisible = true;
      resultSrcText = null;
      inputSrcText = null;
      jsFiles = null;
      cvtUploadModel.inputErrors = null;
   }

   static class CodeUploadResult {
      JavaModel model;
      String error;
   }

   CodeUploadResult addUploadedFile(String fileName) {
      inputSrcText = FileUtil.getFileAsString(fileName);
      JavaModel model = doUpload();
      CodeUploadResult res = new CodeUploadResult();
      if (model == null) {
         res.error = cvtUploadModel.inputErrors;
      }
      else {
         res.model = model;
      }
      return res;
   }

   void sendConvertEvent(int numFiles, int numLines, int numErrors, boolean useModules) { 
   }

   void logError(String error) {
      System.err.println("*** Error running java to JS converter: " + error);
   }

   void acceptedTerms() {
      showTerms = false;
   }
}

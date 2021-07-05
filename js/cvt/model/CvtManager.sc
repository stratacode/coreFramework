import sc.parser.Language;
import sc.parser.ParseError;
import sc.parser.ParseUtil;
import sc.lang.JavaLanguage;
import sc.lang.js.JSLanguage;
import sc.lang.SCLanguage;
import sc.lang.java.JavaModel;

import java.util.Arrays;
import java.util.List;

import sc.layer.LayeredSystem;
import sc.layer.LayerUtil;
import sc.layer.Options;
import sc.layer.SystemContext;

import sc.util.FileUtil;

/**
 * Global, shared component that provides language conversion services - from language code to language code
 */
@Component
object CvtManager {
   static String cvtLibURL = "/javaToJS/libFiles/";

   static class CvtImpl {
      // List of layers to include in the definition of the LayeredSystem used to do the conversion
      String[] convertLayerList;
      InputLanguage fromLang;
      LayeredSystem sys;
      String jsPrefix;

      CvtImpl(String[] layerList, InputLanguage from) {
         convertLayerList = layerList;
         fromLang = from;
      }
   }

   CvtImpl[] convertImpls = {
      new CvtImpl(new String[]{"js.cvt.javaToJS"}, InputLanguage.Java),
      new CvtImpl(new String[]{"js.cvt.javaToJSNoModules"}, InputLanguage.Java)
   };

   CvtImpl getCvtImpl(String layerName) {
      String layerDotName = layerName.replace('_', '.');

      for (CvtImpl impl: convertImpls) {
         if (impl.convertLayerList[impl.convertLayerList.length-1].equals(layerDotName))
            return impl;
      }
      System.err.println("*** No layer conversion registered for layerName: " + layerName);
      return null;
   }


// List of layers to use to build the convertSystems - one for each converter config

   void init() {
      int ix = 0;
      LayeredSystem currentSys = LayeredSystem.getCurrent();
      try {
         if (currentSys != null)
            currentSys.acquireDynLock(false);
         for (CvtImpl cvtImpl:convertImpls) {
            Options options = new Options();
            if (cvtImpl.sys != null) {
               System.err.println("*** Unable to reinit CvtManager!");
               continue;
            }
            options.installLayers = false;
            String layerPath;
            if (currentSys == null) {
               options.scInstallDir = "/usr/local/scc";
               options.mainDir = "/usr/local/scMain";
               layerPath = LayerUtil.getLayerPathFromMainDir(options.mainDir);
            }
            else {
               options.scInstallDir = currentSys.options.scInstallDir;
               options.mainDir = currentSys.options.mainDir;
               layerPath = currentSys.layerPath;
            }
            // Need the keep the parse nodes around after transform for memory
            options.clearParseNodes = false;
            List cvtLayerList = Arrays.asList(cvtImpl.convertLayerList);
            // Create a new system using the same layer path
            LayeredSystem sys = new LayeredSystem(cvtLayerList, null, layerPath, options, null, null, false, null, new SystemContext());
            cvtImpl.sys = sys;

            if (!sys.buildSystem(null, false, true)) {
               System.err.println("*** Prebuild system for layer list: " + cvtLayerList + " failed");
            }
            else {
               if (!sys.buildSystem(null, false, false)) {
                  System.err.println("*** Build system for layer list: " + cvtLayerList + " failed");
               }
               sys.buildCompleted(true);
            }
            LayeredSystem errorSys = currentSys != null ? currentSys : sys;
            if (sys.anyErrors) {
               errorSys.error("Build system for layer list: " + cvtLayerList + " failed");
            }
            else
               errorSys.info("Build system for layer list: " + cvtLayerList + " successful");

            cvtImpl.jsPrefix = cvtLibURL + sys.buildLayer.layerName.replace('.', '_') + "/";
         }
      }
      finally {
         if (currentSys != null) {
            LayeredSystem.setCurrent(currentSys);
            currentSys.releaseDynLock(false);

         }
      }
   }

   String getJSLibFile(String layerName, String jsLibFile) {
      CvtImpl impl = getCvtImpl(layerName); 
      if (impl == null) {
          return null;
      }
      return FileUtil.concat(impl.sys.buildLayer.buildDir, "web", jsLibFile);
   }

   enum InputLanguage {
      Java {
         JavaLanguage getInputLanguage() {
            return sc.lang.JavaLanguage.getJavaLanguage();
         }
      },
      StrataCode {
         JavaLanguage getInputLanguage() {
               return sc.lang.SCLanguage.getSCLanguage();
         }
      };

      abstract JavaLanguage getInputLanguage();
   }

   enum OutputLanguage {
      Java,
      JavaScript
   }

}

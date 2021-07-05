import java.util.Map;

import sc.servlet.UploadResult;
import sc.util.FileUtil;

import java.io.File;

@URL(pattern="/codeUpload", mimeType="text/plain", testScripts={"none"})
scope<request>
class CodeUploadPage extends UploadPage {
   uploadPath = "/tmp/codeUpload";

   CvtUploadModel cvtUploadModel = CvtUploadModel;

   UploadResult processUpload(Map<String,String> uploadedFiles, Map<String,String> formFields) {
      if (cvtUploadModel == null) {
         return new UploadResult(null, "No CvtForm found for upload");
      }

      List<String> errors = new ArrayList<String>();
      List<String> newTypes = new ArrayList<String>();

      for (Map.Entry<String,String> ent:uploadedFiles.entrySet()) {
         String fileNameWithExt = ent.getValue();
         String uploadFileName = FileUtil.concat(uploadPath, fileNameWithExt);

         if (new File(uploadFileName).length() == 0) {
            errors.add("Skipping uploaded file of zero length: " + fileNameWithExt);
            continue;
         }

         CvtForm.CodeUploadResult res = cvtUploadModel.cvtForm.addUploadedFile(uploadFileName);

         if (res.error != null) {
            errors.add(res.error);
            continue;
         }
         else

         new File(uploadFileName).delete();

         newTypes.add(res.model.getModelTypeName());
      }

      if (errors.size() != 0)
         cvtUploadModel.inputErrors = errorsToString(errors);

      return new UploadResult(newTypes.size() > 0 ? newTypes : null, errors.size() == 0 ? null : errorsToString(errors));
   }

   String errorsToString(List<String> errors) {
      if (errors.size() == 1)
         return errors.get(0);
      else
         return "Multiple errors: " + errors;
   }
}

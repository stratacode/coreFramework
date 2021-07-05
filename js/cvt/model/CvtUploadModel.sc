import sc.lang.java.JavaModel;
import java.util.List;

// TODO: Needs to be a session scoped component so there's one instance shared by the request scoped CvtUploadPage
// and the window scoped CvtForm
object CvtUploadModel {
   List<JavaModel> uploadedModels = null;
   String inputErrors;

   // The last cvtForm to use touch this session
   CvtForm cvtForm;
}
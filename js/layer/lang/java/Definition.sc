import java.util.Map;
import sc.obj.GetSet;

class Definition {
   @GetSet
   Map<String,Object> annotations;

   static String getAnnotationValueKey(String typeName, String ident) {
      return typeName + "__" + ident;
   }

   Object getAnnotationValue(String annotName, String valName) {
      Map<String,Object> annots = annotations;
      if (annots != null) {
         return annots.get(getAnnotationValueKey(annotName, valName));
      }
      return null;
   }
}

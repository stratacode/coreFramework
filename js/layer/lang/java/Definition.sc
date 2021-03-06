import java.util.Map;
import sc.obj.GetSet;

class Definition {
   @GetSet
   Map<String,Object> annotations;

   @sc.obj.Constant
   @GetSet
   int modifierFlags;

   @sc.obj.Constant
   @GetSet
   String enclosingTypeName;

   static String getAnnotationValueKey(String typeName, String ident) {
      return typeName + "__" + ident;
   }

   Object getAnnotationValue(String annotName, String valName) {
      Map<String,Object> annots = annotations;
      if (annots != null) {
         if (sc.type.PTypeUtil.isUndefined(annots))
            System.out.println("*** Corrupt annotation! should not be null");
         return annots.get(getAnnotationValueKey(annotName, valName));
      }
      return null;
   }

   boolean hasAnnotation(String annotName) {
      Map<String,Object> annots = annotations;
      if (annots != null) {
         return annots.get(annotName) != null;
      }
      return false;
   }

   boolean hasModifier(String modName) {
      return sc.type.Modifier.hasModifier(modifierFlags, modName);
   }
}

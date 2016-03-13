import sc.layer.Layer;
import sc.lang.sc.PropertyAssignment;
import sc.dyn.DynUtil;
import java.util.ArrayList;
import sc.layer.LayeredSystem;

public class ModelUtil {
   public static Layer getLayerForType(LayeredSystem sys, Object type) {
      if (type instanceof TypeDeclaration)
         return ((TypeDeclaration) type).layer;
      return null;
   }

   public static String getPackageName(Object type) {
      if (type instanceof TypeDeclaration)
         return ((TypeDeclaration) type).packageName;
      else 
         return sc.dyn.DynUtil.getPackageName(type);
   }

   public static String getPropertyName(Object type) {
      if (type instanceof String)
         return (String) type;
      if (type instanceof VariableDefinition) 
         return ((VariableDefinition) type).variableName;
      if (type instanceof PropertyAssignment)
         return ((PropertyAssignment) type).propertyName;
      if (type instanceof BodyTypeDeclaration)  // enum constants
         return ((BodyTypeDeclaration) type).typeName;
      else
         throw new UnsupportedOperationException();
   }

   public static String getInnerTypeName(Object type) {
      if (type instanceof TypeDeclaration)
         return ((TypeDeclaration)type).typeName; // TODO: should be the full inner type name
      return DynUtil.getInnerTypeName(type);

   }

   public static boolean isLayerType(Object type) {
      return type instanceof BodyTypeDeclaration && ((BodyTypeDeclaration) type).isLayerType;
   }

   public static boolean isProperty(Object type) {
      return type instanceof VariableDefinition || type instanceof PropertyAssignment ||
         (type instanceof BodyTypeDeclaration && ((BodyTypeDeclaration) type).isEnumConstant());
   }

   public static Object getPropertyType(Object prop) {
      if (prop instanceof VariableDefinition) {
         return DynUtil.findType(((VariableDefinition) prop).variableTypeName);
      }
      else if (prop instanceof PropertyAssignment) {
         return DynUtil.findType(((PropertyAssignment) prop).variableTypeName);
      }
      return null;
   }

   public static boolean isDynamicType(Object type) {
      return type instanceof TypeDeclaration && ((TypeDeclaration) type).isDynamicType();
   }

   public static boolean isEnumType(Object type) {
      return type instanceof TypeDeclaration && ((TypeDeclaration) type).getDeclarationType() == DeclarationType.ENUM;
   }

   public static boolean isObjectType(Object type) {
      return type instanceof TypeDeclaration && ((TypeDeclaration) type).getDeclarationType() == DeclarationType.OBJECT;
   }

   public static String getTypeName(Object type) {
      if (type instanceof TypeDeclaration)
         return ((TypeDeclaration)type).fullTypeName;
      return DynUtil.getTypeName(type, false);
   }

   public static boolean hasModifier(Object type, String modName) {
      if (type instanceof TypeDeclaration) {
         ArrayList<String> modifiers = ((TypeDeclaration) type).getClientModifiers();
         if (modifiers != null) {
            return modifiers.indexOf(modName) != -1;
         }
      }
      return false;
   }

   public static ClientTypeDeclaration getClientTypeDeclaration(Object type) {
      if (type instanceof ClientTypeDeclaration)
         return (ClientTypeDeclaration) type;
      return null;
   }

}

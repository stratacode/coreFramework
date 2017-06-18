import sc.layer.Layer;
import sc.lang.sc.PropertyAssignment;
import sc.dyn.DynUtil;
import sc.type.CTypeUtil;
import java.util.ArrayList;
import java.util.List;
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

   public static Layer getPropertyLayer(Object prop) {
      if (prop instanceof String)
         return null;
      if (prop instanceof VariableDefinition)
         return ((VariableDefinition) prop).layer;
      if (prop instanceof PropertyAssignment)
         return ((PropertyAssignment) prop).layer;
      if (prop instanceof BodyTypeDeclaration)  // enum constants
         return ((BodyTypeDeclaration) prop).layer;
      else
         throw new UnsupportedOperationException();
   }

   public static Object getAnnotationValue(Object def, String annotName, String valName) {
       if (def instanceof Definition) {
          return ((Definition) def).getAnnotationValue(annotName, valName);
       }
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

   public static boolean isEnum(Object type) {
      return type instanceof TypeDeclaration && ((TypeDeclaration) type).getDeclarationType() == DeclarationType.ENUMCONSTANT;
   }

   public static boolean isInterface(Object type) {
      return type instanceof InterfaceDeclaration;
   }

   public static boolean isObjectType(Object type) {
      return type instanceof TypeDeclaration && ((TypeDeclaration) type).getDeclarationType() == DeclarationType.OBJECT;
   }

   public static String getClassName(Object type) {
      return CTypeUtil.getClassName(getTypeName(type));
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

   public boolean filteredProperty(Object type, Object p, boolean perLayer) {
      // TODO: can implement special rules here, like on the server but for the most part our properties should already reflect
      // the ones we want to display on the client
      return false;
   }


   public static Object getPreviousDefinition(Object def) {
       // TODO: should get enclosing type of the definition and find the same type, member, etc. in the layer before this one
      return null;
   }

   // TODO: slightly modified version from ModelUtil.java
   public static List mergeProperties(List modProps, List declProps, boolean replace, boolean includeAssigns) {
      if (modProps == null)
         return declProps;
      if (declProps == null)
         return modProps;
      if (!(modProps instanceof ArrayList) && declProps.size() > 0)
         modProps = new ArrayList(modProps);
      for (int i = 0; i < declProps.size(); i++) {
         Object prop = declProps.get(i);
         if (prop == null)
            continue;
         if (includeAssigns && isReverseBinding(prop))
            modProps.add(prop);
         else {
            int ix = propertyIndexOf(modProps, prop, true);
            if (ix == -1)
               modProps.add(prop);
            else if (replace)
               modProps.set(ix, prop);
            /*  In LayerSyncHandler, MethodDefinitions are turned into VariableDefinitions for serialization to the client - if this distinction
                is important on the client, we could mark the VariableDefinitions or create a new class for get/set methods on the client
            else {
               Object modProp = modProps.get(ix);
               if (ModelUtil.isGetMethod(prop) && ModelUtil.isField(modProp))
                  modProps.set(ix, prop);
            }
            */
         }
      }
      return modProps;
   }

   // TODO: a direct copy from ModelUtil.java
   public static int propertyIndexOf(List props, Object prop, boolean byName) {
      if (props == null || prop == null)
         return -1;
      for (int i = 0; i < props.size(); i++) {
         Object cprop = props.get(i);
         if (cprop == null)
            continue;
         if (byName) {
            String name = ModelUtil.getPropertyName(cprop);
            if (name.equals(ModelUtil.getPropertyName(prop)))
               return i;
         }
         else {
            if (prop == cprop)
               return i;
         }
      }
      return -1;
   }

   public static boolean isReverseBinding(Object def) {
      if (!(def instanceof PropertyAssignment))
         return false;

      PropertyAssignment pa = (PropertyAssignment) def;
      return pa.operatorStr != null && pa.operatorStr.equals("=:");
   }
}

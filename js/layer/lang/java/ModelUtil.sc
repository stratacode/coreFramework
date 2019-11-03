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

   public static Layer getLayerForMember(LayeredSystem sys, Object member) {
      if (member instanceof VariableDefinition)
         return ((VariableDefinition) member).layer;
      else if (member instanceof BodyTypeDeclaration)
         return ((BodyTypeDeclaration) member).layer;
      else if (member instanceof PropertyAssignment)
         return ((PropertyAssignment) member).layer;
      return null;
   }

   public static String getPackageName(Object type) {
      if (type instanceof TypeDeclaration)
         return ((TypeDeclaration) type).packageName;
      else 
         return sc.dyn.DynUtil.getPackageName(type);
   }

   public static String getExtendsTypeName(Object type) {
      if (type instanceof TypeDeclaration)
         return ((TypeDeclaration) type).extendsTypeName;
      else {
         Object extType = DynUtil.getExtendsType(type);
         return extType == null ? null : DynUtil.getTypeName(extType, false);
      }
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

   public static boolean hasAnnotation(Object def, String annotName) {
      if (def instanceof Definition) {
         return ((Definition) def).hasAnnotation(annotName);
      }
      else
         return DynUtil.hasAnnotation(def, annotName);
   }

   public static Object getAnnotationValue(Object def, String annotName, String valName) {
      if (def instanceof Definition) {
         return ((Definition) def).getAnnotationValue(annotName, valName);
      }
      else
         return DynUtil.getAnnotationValue(def, annotName, valName);
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
      return getPropertyType(prop, null);
   }

   public static Object findTypeDeclaration(LayeredSystem sys, String typeName, Layer refLayer, boolean layerResolve) {
      return sys.getSrcTypeDeclaration(typeName, refLayer);
   }

   public static Object findType(LayeredSystem sys, String typeName) {
      Object res = findTypeDeclaration(sys, typeName, null, false);
      if (res != null)
         return res;
      return DynUtil.findType(typeName);
   }

   public static Object getPropertyType(Object prop, LayeredSystem sys) {
      if (prop instanceof VariableDefinition) {
         return DynUtil.findType(((VariableDefinition) prop).variableTypeName);
      }
      else if (prop instanceof PropertyAssignment) {
         return DynUtil.findType(((PropertyAssignment) prop).variableTypeName);
      }
      return null;
   }

   public static Object getEnclosingType(Object prop, LayeredSystem sys) {
      if (prop instanceof Definition) {
         String enclTypeName = ((Definition) prop).enclosingTypeName;
         if (enclTypeName == null)
            return null;
         return findType(sys, enclTypeName);
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

   public static Object[] getEnumConstants(Object type) {
      if (type instanceof TypeDeclaration)
         return ((TypeDeclaration) type).getEnumValues();
      else
         return DynUtil.getEnumConstants(type);
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

   public static Object getArrayOrListComponentType(Object arrType) {
      return DynUtil.getComponentType(arrType);
   }

   public static boolean hasModifier(Object def, String modName) {
      if (def instanceof Definition)
         return ((Definition) def).hasModifier(modName);
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

   public static boolean isCompiledClass(Object type) {
      return type instanceof Class;
   }

   public static Object resolveSrcTypeDeclaration(LayeredSystem sys, Object type) {
      Object srcRes = findTypeDeclaration(sys, ModelUtil.getTypeName(type), null, false);
      if (srcRes != null)
         return srcRes;
      return type;
   }

   public static boolean sameTypes(Object t1, Object t2) {
      return ModelUtil.getTypeName(t1).equals(ModelUtil.getTypeName(t2));
   }

   public static String getOperator(Object prop) {
      if (prop instanceof IVariableInitializer) {
         String op = ((IVariableInitializer) prop).operatorStr;
         if (op == null)
            return " = ";
         return op;
      }
      throw new IllegalArgumentException();
   }
}

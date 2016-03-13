/*
 * Copyright (c) 2009. Jeffrey Vroom
 */

import sc.type.CTypeUtil;
import sc.obj.Constant;
import java.util.List;
import java.util.Collection;

/** The client view of this file (a subset of the original one - yes, should be using layers to keep them in sync but that will require needing to build SC with SC which adds a tooling challenge! */
@sc.obj.Sync(onDemand=true)
public class Layer {
   /** Prepend this prefix globally to all package names auto-generated */
   @Constant public String packagePrefix = "";

   /** Set using the public or private modifier set on the layer definition itself */
   public String defaultModifier;

   /** Is this a compiled or a dynamic layer */
   @Constant public boolean dynamic;

   /** Set this to true for any layers you do not want to show up in the UI */
   @Constant public boolean hidden = false;

   /** Set to true for layers that cannot be used in dynamic mode.  Default for them is to compile them. */
   @Constant public boolean compiledOnly = false;

   /** Set to true for layers who want to show all objects and properties visible in their extended layers */
   @Constant public boolean transparent = false;

   /** Contains the list of layers this layer extends */
   @Constant public List<String> baseLayerNames;

   /** Set by the system to the full path to the directory containing LayerName.sc (layerFileName = layerPathName + layerBaseName) */
   @Constant public String layerPathName;

   /** Just the LayerName.sc part */
   @Constant public String layerBaseName;

   /** The unique name of the layer - prefix + dirName, e.g. sc.util.util */
   @Constant public String layerUniqueName;

   /** The name of the layer used to find it in the layer path dot separated, e.g. groupName.dirName */
   @Constant public String layerDirName;

   /** The integer position of the layer in the list of layers */
   @Constant public int layerPosition;

   @Constant public CodeType codeType = CodeType.Application;

   @Constant public CodeFunction codeFunction = CodeFunction.Program;

   /** The vanilla name of the layer - no package prefix but does include the layer group */
   @Constant public String getLayerName() {
      String base = layerDirName;
      if (base == null || base.equals(".")) {
         if (packagePrefix != null && packagePrefix.length() > 1 && layerUniqueName != null)
            base = layerUniqueName.substring(packagePrefix.length()+1);
         else
            base = layerUniqueName;
      }
      return base;
   }

   /** Just the layer group */
   public String getLayerGroupName() {
      if (layerDirName == null)
         return null;
      return CTypeUtil.getPackageName(layerDirName);
   }

   public String toString() {
      String base = getLayerName();
      if (packagePrefix == null || packagePrefix.length() == 0)
         return base;
      else
         return base + "(" + packagePrefix + ")";
   }

   public String toDetailString() {
      StringBuilder sb = new StringBuilder();
      if (dynamic)
         sb.append("dynamic ");
      if (hidden)
         sb.append("hidden ");
      sb.append(toString());

      if (codeType != null) {
         sb.append(" codeType: ");
         sb.append(codeType);
      }

      if (codeFunction != null) {
         sb.append(" codeFunction: ");
         sb.append(codeFunction);
      }
      sb.append(" #");
      sb.append(layerPosition);

      if (dependentLayers != null) {
         sb.append(" used by: ");
         sb.append(dependentLayers);
      }

      return sb.toString();
   }

   List<Layer> dependentLayers;
   public List<Layer> getDependentLayers() {
      return dependentLayers;
   }
   public void setDependentLayers(List<Layer> layers) {
      dependentLayers = layers;
   }

   public boolean matchesFilter(Collection<CodeType> codeTypes, Collection<CodeFunction> codeFunctions) {
      return (codeTypes == null || codeTypes.contains(codeType)) && (codeFunctions == null || codeFunctions.contains(codeFunction));
   }

   // Need this to match the compiled class on the server.  Hard to ensure we always process against the source version of this built
   // in class
   public void setLayerUniqueName(String str) {
      layerUniqueName = str;
   }
   public String getLayerUniqueName() {
      return layerUniqueName;
   }

}

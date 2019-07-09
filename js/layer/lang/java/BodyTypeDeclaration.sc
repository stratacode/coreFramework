import java.util.List;
import java.util.ArrayList;
import sc.layer.Layer;
import sc.layer.LayeredSystem;
import sc.obj.Constant;

// A version of the BodyTypeDeclaration for clients that cannot run the full dynamic runtime.
// Essentially all of the TypeDeclaration classes store their info in this one value object so 
// we can do basic operations on the metadata of the code, without exposing source code and
// the complete model of the code.
class BodyTypeDeclaration extends Definition {
   String typeName;
   boolean isLayerType;

   public String getTypeName() {
      return typeName;
   }

   private String fullTypeName;
   @Constant
   public void setFullTypeName(String ftn) {
      fullTypeName = ftn;

      if (ftn != null && LayeredSystem.current != null)
         LayeredSystem.current.addTypeDeclaration(fullTypeName, this);
   }
   public String getFullTypeName() {
      return fullTypeName;
   }

   private String extendsTypeName;
   @Constant
   public void setExtendsTypeName(String ext) {
      extendsTypeName = ext;
   }
   public String getExtendsTypeName() {
      return extendsTypeName;
   }

   private DeclarationType declarationType;
   @Constant
   public void setDeclarationType(DeclarationType dt) {
      declarationType = dt;
   }
   public DeclarationType getDeclarationType() {
      return declarationType;
   }

   private List<Object> declaredProperties;
   @Constant
   public List<Object> getDeclaredProperties() {
      return declaredProperties;
   }
   public void setDeclaredProperties(List<Object> ap) {
      declaredProperties = ap;
      markChanged();
   }

   private String constructorParamNames;
   @Constant
   public String getConstructorParamNames() {
      return constructorParamNames;
   }
   public void setConstructorParamNames(String cpn) {
      constructorParamNames = cpn;
      markChanged();
   }

   private AbstractMethodDefinition editorCreateMethod;
   @Constant
   public AbstractMethodDefinition getEditorCreateMethod() {
      return editorCreateMethod;
   }
   public void setEditorCreateMethod(AbstractMethodDefinition meth) {
      editorCreateMethod = meth;
   }

   private String packageName;
   @Constant
   public void setPackageName(String pn) {
      packageName = pn;
   }

   public String getPackageName() {
      return packageName;
   }

   private String scopeName;
   @Constant
   public void setScopeName(String sn) {
      scopeName = sn;
   }

   public String getScopeName() {
      return scopeName;
   }

   private boolean dynamicType;
   @Constant
   public void setDynamicType(boolean dt) {
      dynamicType = dt;
   }

   public boolean isDynamicType() {
      return dynamicType;
   }

   Layer layer;
   @Constant
   public void setLayer(Layer l) {
      layer = l;
   }
   public Layer getLayer() {
      return layer;
   }

   String comment;
   @Constant
   public void setComment(String c) {
      comment = c;
   }
   public String getComment() {
      return comment;
   }

   /*
   ArrayList<String> clientModifiers;
   @Constant
   public ArrayList<String> getClientModifiers() {
      return clientModifiers;
   }
   public void setClientModifiers(ArrayList<String> newMods) {
      clientModifiers = newMods;
   }
   */

   @Constant
   public boolean isEnumConstant() {
      return declarationType == DeclarationType.ENUMCONSTANT;
   }

   private boolean existsInJSRuntime;
   @Constant
   void setExistsInJSRuntime(boolean dt) {
      existsInJSRuntime = dt;
   }

   boolean getExistsInJSRuntime() {
      return existsInJSRuntime;
   }

   void markChanged() {
      sc.bind.Bind.sendChangedEvent(this, null);
   }

   // TODO: fix me - at least need to filter out only the enum constants here?
   public Object[] getEnumValues() {
      return declaredProperties == null ? null : declaredProperties.toArray();
   }

}

import java.util.List;
import java.util.ArrayList;
import sc.layer.Layer;
import sc.obj.Constant;

class BodyTypeDeclaration {
   String typeName;
   boolean isLayerType;

   public String getTypeName() {
      return typeName;
   }

   private String fullTypeName;
   @Constant
   public void setFullTypeName(String ftn) {
      fullTypeName = ftn;
   }
   public String getFullTypeName() {
      return fullTypeName;
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

   private String packageName;
   @Constant
   public void setPackageName(String pn) {
      packageName = pn;
   }

   public String getPackageName() {
      return packageName;
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

   ArrayList<String> clientModifiers;
   @Constant
   public ArrayList<String> getClientModifiers() {
      return clientModifiers;
   }
   public void setClientModifiers(ArrayList<String> newMods) {
      clientModifiers = newMods;
   }

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

}

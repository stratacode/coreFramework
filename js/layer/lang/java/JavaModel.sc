import sc.bind.IChangeable;
import sc.bind.Bind;
import sc.bind.IListener;

import sc.layer.Layer;
import sc.layer.SrcEntry;

import sc.obj.Constant;

@sc.obj.Sync(onDemand=true)
public class JavaModel implements IChangeable {
   //public String modelTypeName;

   private Layer layer;
   public void setLayer(Layer l) {
      layer = l;
   }
   public Layer getLayer() {
      return layer;
   }

   public JavaModel resolve() {
      return this;
   }

   private boolean existsInJSRuntime;
   @Constant
   public void setExistsInJSRuntime(boolean v) {
      existsInJSRuntime = v;
   }
   public boolean getExistsInJSRuntime() {
      return existsInJSRuntime;
   }

   public SrcEntry srcFile;

   @Bindable
   public String cachedModelText;

   @Bindable
   public boolean needsModelText;

   @Bindable
   @sc.obj.HTMLSettings(returnsHTML=true)
   public String cachedHTMLModelText;

   @Bindable
   public boolean needsHTMLModelText;

   @Bindable
   @sc.obj.HTMLSettings(returnsHTML=true)
   public String cachedGeneratedText;

   @Bindable
   public String cachedGeneratedJSText;

   @Bindable
   @sc.obj.HTMLSettings(returnsHTML=true)
   public String cachedGeneratedSCText;

   @Bindable
   @sc.obj.HTMLSettings(returnsHTML=true)
   public String cachedGeneratedClientJavaText;

   @Bindable
   public boolean needsGeneratedText;

   public TypeDeclaration layerTypeDeclaration;

   public TypeDeclaration getLayerTypeDeclaration() {
      return layerTypeDeclaration;
   }

   public void setLayerTypeDeclaration(TypeDeclaration ltd) {
      layerTypeDeclaration = ltd;
   }

   public ClientTypeDeclaration getClientLayerTypeDeclaration() {
      return (ClientTypeDeclaration) layerTypeDeclaration;
   }

   // TODO: Change tracking... server increments some version number and we send the IChangeable event

   // Used to fire refresh events on the source or generated text events on this file
   public void markChanged() {
      Bind.sendEvent(IListener.VALUE_CHANGED, this, null);
      Bind.sendChangedEvent(this, "cachedGeneratedJSText");
      Bind.sendChangedEvent(this, "cachedGeneratedText");
      Bind.sendChangedEvent(this, "cachedModelText");
      Bind.sendChangedEvent(this, "cachedHTMLModelText");
      Bind.sendChangedEvent(this, "cachedGeneratedSCText");
      Bind.sendChangedEvent(this, "cachedGeneratedClientJavaText");
   }
}

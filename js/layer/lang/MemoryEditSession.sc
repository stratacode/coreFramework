package sc.lang;

import sc.lang.java.JavaModel;

@sc.obj.Sync(onDemand=true)
public class MemoryEditSession implements sc.obj.IObjectId {
   @Bindable
   public String origText; // Text when the editor session started
   @Bindable
   public String text;  // Current text
   public JavaModel model;
   @Bindable
   public boolean saved; // Have we saved this since origText was set
   @Bindable
   public int caretPosition; // Save the current spot in the text
   @Bindable
   public boolean cancelled; // Set to true when this edit session has been reverted

   String getObjectId() {
     return "MES_" + (model == null ? "null" : sc.type.CTypeUtil.escapeIdentifierString(model.getLayer().getLayerName() + "__" + model.srcFile.relFileName));
   }
}

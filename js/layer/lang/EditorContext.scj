import sc.bind.Bind;
import sc.layer.Layer;

import sc.lang.ClientEditorContext;
import sc.lang.java.ModelUtil;

import java.util.ArrayList;
import java.util.List;

import sc.dyn.DynUtil;

@sc.obj.Sync(onDemand=true)
public class EditorContext extends ClientEditorContext {
   Layer currentLayer;
   Object currentType;

   public List<InstanceWrapper> getInstancesOfType(Object type, int max, boolean addNull) {
      ArrayList<InstanceWrapper> ret = new ArrayList<InstanceWrapper>();
      if (type == null)
         return ret;
      String typeName = ModelUtil.getTypeName(type);
      Object[] insts = DynUtil.getInstancesOfTypeAndSubTypes(typeName);
      int i = 0;
      // Add a null entry at the front to represent the <type> selection
      if (addNull)
         ret.add(new InstanceWrapper(this, null));

      // TODO: need a way to test for jv_Enum and then get the instance
      //if (ModelUtil.isEnum(type)) {
      //   ret.add(new InstanceWrapper(this, ModelUtil.getRuntimeEnum(type)));
      //}

      if (insts == null)
         return ret;

      int sz = insts.length;
      for (i = 0; i < max && i < sz; i++) {
         ret.add(new InstanceWrapper(this, insts[i]));
      }

      if (sz == 1 && DynUtil.getEnclosingType(type, true) == null && DynUtil.isObjectType(type)) {
         ret.add(new InstanceWrapper(this, type != null, typeName)); // A dummy wrapper which creates the instance when it is selected
      }

      return ret;
   }

   private boolean needsSave = false;
   @Bindable(manual=true)
   public boolean getNeedsSave() {
      return needsSave;
   }

   public void setNeedsSave(boolean val) {
      needsSave = val;
      Bind.sendChangedEvent(this, "needsSave");
   }

   private boolean canUndo = false;
   @Bindable(manual=true)
   public boolean getCanUndo() {
      return canUndo;
   }
   public void setCanUndo(boolean val) {
      canUndo = val;
      Bind.sendChangedEvent(this, "canUndo");
   }

   private boolean canRedo = false;
   @Bindable(manual=true)
   public boolean getCanRedo() {
      return canRedo;
   }
   public void setCanRedo(boolean val) {
      canRedo = val;
      Bind.sendChangedEvent(this, "canRedo");
   }

}

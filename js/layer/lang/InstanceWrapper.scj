package sc.lang;

import sc.dyn.DynUtil;

public class InstanceWrapper {
   EditorContext ctx;
   Object theInstance;
   boolean canCreate = false;
   String typeName;
   public InstanceWrapper(EditorContext ctx, boolean canCreate, String typeName) {
      this.typeName = typeName;
      this.canCreate = canCreate;
      this.ctx = ctx;
   }

   public InstanceWrapper(EditorContext ctx, Object inst) {
      this.theInstance = inst;
      this.ctx = ctx;
   }

   public Object getInstance() {
      if (theInstance != null)
         return theInstance;

      if (canCreate)
         return theInstance = DynUtil.resolveName(typeName, true);

      return null;
   }

   public String toString() {
      if (theInstance == null)
         return typeName != null ? (canCreate ? "<select to create>" : "<restart to create>") : "<type>";
      return DynUtil.getInstanceName(theInstance);
   }

   public boolean equals(Object other) {
      if (other instanceof InstanceWrapper) {
         InstanceWrapper otherInst = (InstanceWrapper) other;
         if (!DynUtil.equalObjects(typeName, otherInst.typeName))
            return false;
         return DynUtil.equalObjects(otherInst.theInstance, theInstance);
      }
      return false;
   }

   public int hashCode() {
      if (theInstance == null) return 0;
      return theInstance.hashCode();
   }
}

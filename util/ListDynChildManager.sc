
public class ListDynChildManager implements sc.dyn.IDynChildManager {

   public void initChildren(Object parentObj, Object[] children) {
      List parent = (List) parentObj;
      for (Object obj:children) 
         parent.add(obj);
   }

   public void addChild(Object parentObj, Object childObj) {
      List parent = (List) parentObj;
      parent.add(childObj);
   }

   public void addChild(int ix, Object parentObj, Object childObj) {
      List parent = (List) parentObj;
      parent.add(ix, childObj);
   }
   public boolean removeChild(Object parentObj, Object childObj) {
      List parent = (List) parentObj;
      return parent.remove(childObj);
   }

   public Object[] getChildren(Object parentObj) {
      List parent = (List) parentObj;
      return parent.toArray();
   }

   public boolean getInitChildrenOnCreate() {
      return true;
   }
}

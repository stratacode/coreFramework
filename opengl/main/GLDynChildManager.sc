import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.JFrame;
import java.awt.Window;

public class GLDynChildManager extends SwingDynChildManager {
   public void initChildren(Object parentObj, Object[] children) {
      if (parentObj instanceof IRenderParent) {
         IRenderParent par = (IRenderParent) parentObj;
         for (Object child:children) {
            if (child instanceof IRenderNode)
               par.addChild((IRenderNode) child);
         }
      }
      else 
         super.initChildren(parentObj, children);
   }

   public void addChild(int ix, Object parentObj, Object childObj) {
      if (childObj instanceof IRenderNode) {
         if (parentObj instanceof IRenderParent)
            ((IRenderParent) parentObj).addChild((IRenderNode)childObj);
      }
      else 
         super.addChild(ix, parentObj, childObj);
   }

   public boolean removeChild(Object parentObj, Object childObj) {
      if (childObj instanceof IRenderNode) {
         if (parentObj instanceof IRenderParent)
            return ((IRenderParent) parentObj).removeChild((IRenderNode)childObj);
      }
      else 
         return super.removeChild(parentObj, childObj);

      return false;
   }

   public Object[] getChildren(Object parentObj) {
      if (parentObj instanceof IRenderParent)
         return ((IRenderParent) parentObj).getChildren().toArray();
      return super.getChildren(parentObj);
   }

   @Override
   public boolean getInitChildrenOnCreate() {
      return true;
   }
}

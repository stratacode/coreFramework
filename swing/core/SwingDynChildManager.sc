import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.JFrame;
import java.awt.Window;

public class SwingDynChildManager implements sc.dyn.IDynChildManager {

   public void initChildren(Object parentObj, Object[] children) {
      if (parentObj instanceof JFrame) {
         JFrame frame = (JFrame) parentObj;

         for (Object childObj:children) {
            addChild(parentObj, childObj);
         }
      }
      else if (parentObj instanceof JComponent) {
         JComponent parent = (JComponent) parentObj;
         java.util.List childrenList = (java.util.List) parent.getClientProperty("sc.children");
         if (childrenList != null)
            children = childrenList.toArray();
         for (Object childObj:children) {
            addChild(parentObj, childObj);
         }
      }
      else if (parentObj instanceof Container) {
         Container parent = (Container) parentObj;
         for (Object childObj:children) {
            addChild(parentObj, childObj);
         }
      }
   }

   public void addChild(Object parentObj, Object childObj) {
      addChild(-1, parentObj, childObj);
   }

   public void addChild(int ix, Object parentObj, Object childObj) {
      SwingUtil.addChild(parentObj, childObj, ix);
   }

   public boolean removeChild(Object parentObj, Object childObj) {
      if (childObj instanceof IChildContainer) {
         Object[] children = ((IChildContainer) childObj).getChildren();
         boolean any = false;
         for (Object child:children) {
            if (removeChild(parentObj, child))
               any = true;
         }
         return any;
      }

      if (!(childObj instanceof Component))
         return false;

      Component child = (Component) childObj;
      if (parentObj instanceof JFrame) {
         JFrame frame = (JFrame) parentObj;
         frame.getContentPane().remove(child);
         frame.getContentPane().validate();
         frame.getContentPane().doLayout();
         frame.getContentPane().repaint();
      }
      else if (parentObj instanceof Window) {
         Window parent = (Window) parentObj;
         parent.remove(child);
         parent.doLayout();
         parent.pack();
         return true;
      }
      else if (parentObj instanceof Container) {
         Container parent = (Container) parentObj;
         parent.remove(child);
      }
      return false;
   }

   public Object[] getChildren(Object parentObj) {
      if (parentObj instanceof JComponent) {
         JComponent parent = (JComponent) parentObj;

         synchronized(parent.getTreeLock()) {
            return parent.getComponents();
         }
      }
      else if (parentObj instanceof Container) {
         Container parent = (Container) parentObj;

         synchronized(parent.getTreeLock()) {
            return parent.getComponents();
         }
      }
      throw new UnsupportedOperationException();
   }

   public static Component[] unwrapChildren(Object childObj) {
      Component[] result;
      if (childObj instanceof Component) {
         result = new Component[1];
         result[0] = (Component) childObj;
         return result;
      }
      else if (childObj instanceof IChildContainer) {
         ArrayList<Component> res = new ArrayList<Component>();
         IChildContainer container = (IChildContainer) childObj;
         for (Object c:container.children) {
            if (c instanceof Component)
               res.add((Component) c);
            if (c instanceof IChildContainer)
               res.addAll(java.util.Arrays.asList(unwrapChildren(c)));
         }
         return res.toArray(new Component[res.size()]);
      }
      return null;
   }

   @Override
   public boolean getInitChildrenOnCreate() {
      return true;
   }
}

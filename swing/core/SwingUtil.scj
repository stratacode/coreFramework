package sc.swing;

import javax.swing.SwingUtilities;

import java.awt.*;
import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.text.JTextComponent;
import javax.swing.text.BadLocationException;

import sc.bind.Bind;
import sc.dyn.DynUtil;

/**
 * A set of swing utilities mostly to do with managing parent/child relationships and integration with data binding.
 * Also wrapper methods for point and dimension.
 *
 */
public class SwingUtil {
   public static sc.type.IBeanMapper preferredSizeProp = sc.type.TypeUtil.getPropertyMapping(JComponent.class, "preferredSize");

   public static void addChild(Object parent, Object child) {
      if (parent instanceof JComponent)
         SwingUtil.addChild((JComponent) parent, child, -1);
      else if (parent instanceof Container)
         SwingUtil.addChild((Container) parent, child, -1);
      else
         throw new IllegalArgumentException("Invalid parent type to SwingUtil.addChild(" + parent + ") - must be an awt Container or swing JComponent");
   }

   public static void addChild(Object parent, Object child, int ix) {
      if (parent instanceof JComponent)
         SwingUtil.addChild((JComponent) parent, child, ix);
      else if (parent instanceof Container)
         SwingUtil.addChild((Container) parent, child, ix);
      else
         throw new IllegalArgumentException("Invalid parent type to SwingUtil.addChild(" + parent + ") - must be an awt Container or swing JComponent");
   }

   public static void addChild(Container parent, Object child) {
      SwingUtil.addChild(parent, child, -1);
   }

   public static void addChild(Container parent, Object child, int ix) {
      if (parent instanceof JFrame && ((JFrame) parent).getJMenuBar() == child)
         return;

      // Must be copied because Container and JComponent are incompatible classes
      if (child instanceof IChildContainer) {
         IChildContainer container = (IChildContainer) child;
         container.setParentComponent(parent);
         Object[] children = container.getChildren();
         if (children != null) {
            for (Object subChild:children) {
               SwingUtil.addChild(parent, subChild);
            }
         }
      }
      // Because we do not compile stubs with the IChildContainer interface, we need to do this dynamically
      else if (DynUtil.instanceOf(child, IChildContainer.class)) {
         DynUtil.invokeMethod(child, DynUtil.resolveMethod(DynUtil.getType(child), "setParentComponent", "Ljava/awt/Component;"), parent);
         Object[] children = DynUtil.getObjChildren(child, null, true);
         if (children != null) {
            for (Object subChild:children) {
               SwingUtil.addChild(parent, subChild);
            }
         }
      }
      if (child instanceof java.awt.Component)
         parent.add((java.awt.Component) child, ix);
   }

   public static void addChild(JComponent parent, Object child) {
      addChild(parent, child, -1);
   }

   public static void addChild(JComponent parent, Object child, int ix) {
      if (child instanceof IChildContainer) {
         IChildContainer container = (IChildContainer) child;
         container.setParentComponent(parent);
         Object[] children = container.getChildren();
         if (children != null) {
            for (Object subChild:children) {
               SwingUtil.addChild(parent, subChild);
            }
         }
      }
      else if (DynUtil.instanceOf(child, IChildContainer.class)) {
         DynUtil.invokeMethod(child, DynUtil.resolveMethod(DynUtil.getType(child), "setParentComponent", "Ljava/awt/Component;"), parent);
         Object[] children = DynUtil.getObjChildren(child, null, true);
         if (children != null) {
            for (Object subChild:children) {
               SwingUtil.addChild(parent, subChild);
            }
         }
      }
      if (child instanceof JComponent) {
         if (child instanceof javax.swing.JMenuItem) {
            if (parent instanceof javax.swing.JMenu) {
               ((javax.swing.JMenu) parent).add((javax.swing.JMenuItem) child);
               return;
            }
         }
         if (child instanceof javax.swing.JMenu) {
            if (parent instanceof javax.swing.JMenuBar) {
               ((javax.swing.JMenuBar) parent).add((javax.swing.JMenu) child);
               // MenuBar's size changes after the first element is added.
               Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, parent, preferredSizeProp);
               return;
            }
         }
         // Swing class for JMenu needs to have the separator added to the popup menu.  Unfortunately, there's no
         // way to actually add our separator here so hopefully the defaults are ok :(
         if (child instanceof javax.swing.JSeparator) {
            if (parent instanceof javax.swing.JMenu) {
               ((javax.swing.JMenu) parent).getPopupMenu().addSeparator();
               return;
            }
         }
         JComponent jc = (JComponent) child;
         parent.add(jc, jc.getClientProperty("sc.constraints"), ix);
      }
      else if (child instanceof java.awt.Component) {
         java.awt.Component c = (java.awt.Component) child;
         parent.add(c, ix);
      }
   }

   public static void removeChild(Object parent, Object child) {
      if (parent instanceof Container)
         removeChild((Container) parent, child);
      else if (parent instanceof JComponent)
         removeChild((JComponent) parent, child);
      else
         throw new IllegalArgumentException("Invalid parent type for removeChild:" + parent);
   }

   public static void removeChild(Container parent, Object child) {
      parent.remove((java.awt.Component) child);
   }

   public static void removeChild(JComponent parent, Object child) {
      parent.remove((JComponent) child);
   }

   public static void sendDelayedEvent(final int eventType, final Object obj, final sc.type.IBeanMapper prop) {
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            Bind.sendEvent(eventType, obj, prop);
         }});
   }
   public static Point point(int x, int y) {
       return new Point(x, y);
   }
   public static Dimension dimension(int x, int y) {
       return new Dimension(x, y);
   }
   public static Dimension dimension(Number x, Number y) {
       return new Dimension(x == null ? 0 : x.intValue(), y == null ? 0 : y.intValue());
   }

   public static Dimension subInsets(Dimension dim, Insets insets) {
      return new Dimension(((int)dim.width) - insets.left - insets.right,
			   ((int)dim.height) - insets.top - insets.bottom);
   }

   // Swing defines properties location.x, size.width which are doubles so we need a
   // method which keeps the casts out of the code
   public static Point point(Number x, Number y) {
       return new Point(x == null ? 0 : x.intValue(), y == null ? 0 : y.intValue());
   }

   public static Color averageColors(Color c1, Color c2) {
      return new Color((c1.getRed() + c2.getRed())>> 1, (c1.getGreen() + c2.getGreen())>>1, (c1.getBlue() + c2.getBlue())>>1);
   }

   /*
    *  Attempt to center the line containing the caret at the center of the
    *  scroll pane.
    *
    *  @param component the text component in the sroll pane
    */
   public static void centerLineInScrollPane(JTextComponent component) {
      Container container = SwingUtilities.getAncestorOfClass(JViewport.class, component);

      try {
         Rectangle r = component.modelToView(component.getCaretPosition());
         JViewport viewport = (JViewport)container;
         int extentHeight = viewport.getExtentSize().height;
         int viewHeight = viewport.getViewSize().height;

         int y = Math.max(0, r.y - (extentHeight / 2));
         y = Math.min(y, viewHeight - extentHeight);

         viewport.setViewPosition(new Point(0, y));
      }
      catch(BadLocationException ble) {
         System.err.println("*** invalid caret position for component");
      }
   }
}

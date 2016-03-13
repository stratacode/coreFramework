import java.util.Collections;

import sc.bind.IListener;
import sc.bind.Bind;
import sc.type.TypeUtil;
import sc.type.IBeanMapper;
import sc.dyn.DynUtil;
import sc.dyn.IDynObject;

@Component
@CompilerSettings(mixinTemplate="sc.gui.grid.ListViewMixinTemplate", childTypeParameter="T",
            //objectTemplate="sc.swing.ChildContainerInit",
            //newTemplate="sc.swing.ChildContainerNew",
            dynChildManager="sc.swing.SwingDynChildManager")
abstract class ListView<T> implements IChildContainer {
   /** Used to iterate over the children of this component */
   List<T> values;

   private List<T> oldValues = new ArrayList<T>();

   private List<Object[]> currentComponents = new ArrayList<Object[]>();

   public List<Object> componentList = new ArrayList<Object>();

   values =: refreshChildren();

   private boolean isStarted = false;

   void start() {
      if (parentComponent != null)
         isStarted = true;
      refreshChildren();
   }

   /** Stores the reference to the last child created.  You can use this in data binding expressions to find where the list ends  */
   Object lastComponent;

   boolean visible = true;
   visible =: propagateVisible();

   public Object[] getChildren() {
      ArrayList<Object> res = new ArrayList<Object>();
      if (values != null) {
         for (int i = 0; i < values.size(); i++) {
            Object c = values.get(i);
            if (c instanceof java.awt.Component || c instanceof IChildContainer)
               res.add(c);
         }
      }
      return res.toArray(new Object[res.size()]);
   }

   void propagateVisible() {
       Object[] children = getChildren();
       if (children == null)
          return;
       for (Object child:children) {
          if (child instanceof java.awt.Component)
             ((java.awt.Component)child).setVisible(visible);
          // TODO: really should check for IChildContainer here and propagate the visibility to the results of getChildren
          else if (child instanceof ComponentGroup) {
             ((ComponentGroup) child).visible = visible;
          }
       }
   }

   private java.awt.Component parentComponent;
   void setParentComponent(java.awt.Component comp) {
      parentComponent = comp;
      if (comp != null) {
         isStarted = true;
         refreshChildren();
      }
   }

   java.awt.Component getParentComponent() {
      return parentComponent;
   }

   private void refreshChildren() {
      if (!isStarted)
         return;
      List<T> vals = values;
      if (vals == null)
         vals = Collections.EMPTY_LIST;

      boolean anyComponentsChanged = false;
      int i = 0;
      Object[] prevValue = null;
      Object[] oldPrev = null;
      Object oldLastComponent = lastComponent;
      if (values != null) {
         for (; i < values.size(); i++) {
            T newValue = values.get(i);
            T oldValue = i < oldValues.size() ? oldValues.get(i) : null;
            if (oldValue != null && DynUtil.equalObjects(oldValue, newValue) && DynUtil.equalArrays(prevValue, oldPrev)) {
               prevValue = currentComponents.get(i);
               oldPrev = prevValue;;
               continue;
            }

            Object[] comps = i < currentComponents.size() ? currentComponents.get(i) : null;
            if (comps != null) {
               for (int c = 0; c < comps.length; c++) {
                  Object comp = comps[c];
                  DynUtil.setProperty(comp, "listItemValue", newValue);
                  DynUtil.setProperty(comp, "listItemPrev", prevValue[c]);
               }
               oldValues.set(i, newValue);
            }
            else {
               oldValues.add(newValue);
               comps = initChildrenForElement(newValue, i, prevValue);
               prevValue = comps;
               oldPrev = null;
               currentComponents.add(comps);
               for (Object comp:comps) {
                  addComponent(comp);
               }
               anyComponentsChanged = true;
            }
         }
      }
      while (i < oldValues.size()) {
         Object oldValue = oldValues.get(i);
         Object[] comps = currentComponents.get(i);
         for (Object comp:comps) {
            removeComponent(comp);
         }
         oldValues.remove(i);
         currentComponents.remove(i);
         anyComponentsChanged = true;
      }
      Object newLastComponent = currentComponents.size() == 0 ? null : getLastElement(currentComponents.get(currentComponents.size()-1));
      if (!DynUtil.equalObjects(newLastComponent, oldLastComponent))
         lastComponent = newLastComponent;
      if (anyComponentsChanged) {
         ArrayList<Object> newComponentList = new ArrayList<Object>();
         for (int k = 0; k < currentComponents.size(); k++) {
            Object[] comps = currentComponents.get(k);
            for (int j = 0; j < comps.length; j++) {
               newComponentList.add(comps[j]);
           }
         }
         componentList = newComponentList;
      }
   }
   protected Object[] _initChildrenForElement(Object val, int index, Object[] prev) {
      return null;
   }

   private Object getLastElement(Object[] arr) {
      if (arr.length == 0)
         return null;
      return arr[arr.length-1];
   }

   protected Object[] initChildrenForElement(Object val, int index, Object[] prev) {
      // Add any dynamic children here.  This method gets overridden if this is a compiled class
      Object thisType = DynUtil.getType(this);
      String[] objNames = DynUtil.getObjChildrenNames(thisType, "ListItem");
      int cix = 0;
      ArrayList<Object> children = new ArrayList<Object>();
      if (objNames != null) {
         Object[] objTypes = DynUtil.getObjChildrenTypes(thisType, "ListItem");


         for (int i = 0; i < objNames.length; i++) {
            Object subType = objTypes[i];
            Object inst;

            inst = DynUtil.createInnerInstance(subType, this, null);

            addChild(children, inst, val, index, prev == null ? null : prev[cix++]);
         }
      }
      Object[] _compiledInsts = _initChildrenForElement(val, index, prev);
      if (_compiledInsts != null) {
         for (Object cinst:_compiledInsts) {
            addChild(children, cinst, val, index, prev == null ? null : prev[cix++]);
         }
      }
      return children.toArray(new Object[children.size()]);
   }

   void addChild(ArrayList<Object> children, Object inst, Object val, int index, Object prev) {
      DynUtil.setProperty(inst, "listItemIndex", index);
      DynUtil.setProperty(inst, "listItemValue", val);
      DynUtil.setProperty(inst, "listItemPrev", prev);

      children.add(inst);
   }

   void addComponent(Object inst) {
      if (parentComponent != null) {
         if (parentComponent instanceof Container)
            SwingUtil.addChild((Container) parentComponent, inst);
         else if (parentComponent instanceof JComponent)
            SwingUtil.addChild((JComponent) parentComponent, inst);
      }
   }

   void removeComponent(Object inst) {
      if (parentComponent != null) {
         if (parentComponent instanceof Container)
            SwingUtil.removeChild((Container) parentComponent, inst);
         else if (parentComponent instanceof JComponent)
            SwingUtil.removeChild((JComponent) parentComponent, inst);
      }
      DynUtil.dispose(inst);
   }
}

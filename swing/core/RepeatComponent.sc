import sc.dyn.DynUtil;
import sc.type.PTypeUtil;
import java.util.Arrays;

public abstract class RepeatComponent<T> implements IChildContainer, sc.dyn.IObjChildren {
   java.awt.Component parentComponent;

   /** Either Object[] or List[] */
   public Object repeat;

   /** Either java.awt.Component or IChildContainers of swing components.  Whatever is returned by createRepeatElement. */
   public List<T> repeatComponents = new ArrayList<T>();

   public boolean disableRefresh = false;
   public boolean valid = true; // start out true so the first invalidate triggers the refresh

   public boolean manageChildren = true;

   public boolean visible = true;

   disableRefresh =: invalidate();
   repeat =: invalidate();
   visible =: invalidate();

   // TODO: by default we could define a separate class from the children of this component using an IChildContainer
   // this could create an instance of that generated class.  This is similar to how repeat works in schtml.
   abstract public T createRepeatElement(Object val, int index, Object oldComp);

   // Implement this method to return the repeat value from the repeatComponent.
   abstract public Object getRepeatVar(T repeatComponent);

   // Called when the component's order in the list has changed
   abstract public void setRepeatIndex(T repeatComponent, int ix);

   public void listChanged() {
      if (parentComponent != null) {
         parentComponent.invalidate();
         parentComponent.repaint();
      }
   }

   void invalidate() {
      if (valid && !disableRefresh) {
         // TODO: We used to remove the form when we invalidate it so we do not refresh the old form unnecessarily before rebuildForm's stuff runs
         //removeForm();
         valid = false;

         //System.out.println("*** Invalidating list: " + repeat + " on thread: " + PTypeUtil.getThreadName());

         SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               //System.out.println("*** Refreshing list: " + repeat + " on thread: " + PTypeUtil.getThreadName());
               if (refreshList())
                  listChanged();
               //System.out.println("*** Done refreshing list: " + repeat + " on thread: " + PTypeUtil.getThreadName());
            }
         });

      }
   }

   private int repeatComponentIndexOf(Object repeatComps, int startIx, Object repeatVar) {
      int sz = PTypeUtil.getArrayLength(repeatComps);
      for (int i = startIx; i < sz; i++) {
         T repeatComp = (T) DynUtil.getArrayElement(repeatComps, i);
         Object arrayVal = getRepeatVar(repeatComp);
         if (arrayVal == repeatVar || (arrayVal != null && arrayVal.equals(repeatVar)))
            return i;
      }
      return -1;
   }

   private int repeatElementIndexOf(Object repeat, int startIx, Object repeatVar) {
      int sz = PTypeUtil.getArrayLength(repeat);
      for (int i = startIx; i < sz; i++) {
         Object arrayVal = DynUtil.getArrayElement(repeat, i);
         if (arrayVal == repeatVar || (arrayVal != null && arrayVal.equals(repeatVar)))
            return i;
      }
      return -1;
   }

   public void rebuildList() {
      removeAllElements();
      refreshList();
   }

   public boolean refreshList() {
      valid = true;

      if (!visible) {
         removeAllElements();
         return true;
      }

      Object repeatVal = repeat;
      int sz = repeatVal == null ? 0 : DynUtil.getArrayLength(repeatVal);
      boolean anyChanges = false;

      // TODO: remove this?  We can't disable sync entirely.  We need to turn it on before we call "output" since there can be side-effect changes
      // in there which need to be synchronized.  Now that we do not sync the page objects, this should not be needed anyway.
      // Since these changes are derived from other properties, disable the recording of syn changes here.
      //SyncManager.SyncState oldSyncState = SyncManager.getSyncState();
      try {
         //SyncManager.setSyncState(SyncManager.SyncState.Disabled);

         List<T> components = repeatComponents;
         if (components == null) {
            repeatComponents = components = new ArrayList<T>(sz);
            for (int i = 0; i < sz; i++) {
               Object arrayVal = DynUtil.getArrayElement(repeatVal, i);
               if (arrayVal == null) {
                  System.err.println("Null value for repeat element: " + i + " for: " + this);
                  continue;
               }
               T arrayComp = createRepeatElement(arrayVal, i, null);
               components.add(arrayComp);
               anyChanges = true;
            }
         }
         else {
            for (int i = 0; i < sz; i++) {
               Object arrayVal = DynUtil.getArrayElement(repeatVal, i);
               if (arrayVal == null) {
                  System.err.println("Null value for repeat element: " + i + " for: " + this);
                  continue;
               }
               int curIx = repeatComponentIndexOf(repeatComponents, 0, arrayVal);

               if (curIx == i) // It's at the right spot in repeatTags for the new value of repeat.
                  continue;

               if (i < repeatComponents.size()) {
                  if (i >= components.size())
                     System.out.println("*** Internal error in sync repeat tags!");
                  T oldElem = components.get(i);
                  Object oldArrayVal = getRepeatVar(oldElem);
                  // The guy in this spot is not our guy.
                  if (oldArrayVal != arrayVal && (oldArrayVal == null || !oldArrayVal.equals(arrayVal))) {
                     anyChanges = true;
                     // The current guy is new to the list
                     if (curIx == -1) {
                        // Either replace or insert a row
                        int curNewIx = repeatElementIndexOf(repeatVal, i, oldArrayVal);
                        if (curNewIx == -1) {
                           T newElem = createRepeatElement(arrayVal, i, oldElem);
                           if (oldElem == newElem) {
                              // The createRepeatElement method returned the same object.
                              // Reuse the existing object so this turns into an incremental refresh
                              //oldElem.setRepeatIndex(i);
                              //oldElem.setRepeatVar(arrayVal);
                           }
                           else {
                              // The createRepeatElement method returned a different object
                              // In this case, the newElem tag may not be the same so we need to replace the element.
                              components.remove(i);
                              removeElement(oldElem, i);
                              components.add(i, newElem);
                              insertElement(newElem, i);
                           }
                        }
                        else {
                           // Assert curNewIx > i - if it is less, we should have already moved it when we processed the old guy
                           T newElem = createRepeatElement(arrayVal, i, null);
                           components.add(i, newElem);
                           insertElement(newElem, i);
                        }
                     }
                     // The current guy is in the list but later on
                     else {
                        // Try to delete our way to the old guy so this stays incremental.  But at this point we also delete all the way to the old guy so the move is as short as possible (and to batch the removes in case this ever is used with transitions)
                        int delIx;
                        boolean needsMove = false;
                        for (delIx = i; delIx < curIx; delIx++) {
                           T delElem = components.get(i);
                           Object delArrayVal = getRepeatVar(delElem);
                           int curNewIx = repeatElementIndexOf(repeatVal, i, delArrayVal);
                           if (curNewIx == -1) {
                              T toRem = components.remove(delIx);
                              removeElement(toRem, delIx);
                           }
                           else
                              needsMove = true;
                        }
                        // If we deleted up to the current, we are done.  Otherwise, we need to re-order
                        if (needsMove) {
                           //elemToMove.setRepeatIndex(i);
                           T elemToMove = components.remove(curIx);
                           components.add(i, elemToMove);
                           moveElement(elemToMove, curIx, i);
                        }
                     }
                  }
               }
               else {
                  anyChanges = true;
                  if (curIx == -1) {
                     T arrayElem = createRepeatElement(arrayVal, i, null);
                     components.add(arrayElem);
                     appendElement(arrayElem);
                  }
                  // Otherwise need to move it into its new location.
                  else {
                     T toMove = components.get(curIx);
                     components.add(i, toMove);
                     //toMove.setRepeatIndex(i);
                     moveElement(toMove, curIx, i);
                  }
               }
            }

            while (components.size() > sz) {
               int ix = components.size() - 1;
               T toRem = components.remove(ix);
               removeElement(toRem, ix);
               anyChanges = true;
            }
         }
         if (anyChanges) {
            for (int i = 0; i < components.size(); i++) {
               setRepeatIndex(components.get(i), i);
            }
         }
      }
      finally {
         //SyncManager.setSyncState(oldSyncState);
      }
      return anyChanges;
   }

   private static List<Object> cloneRepeatList(Object list) {
      if (list == null)
         return null;
      else if (list instanceof Object[]) {
         return new ArrayList(Arrays.asList((Object[]) list));
      }
      else if (list instanceof List) {
         return new ArrayList((List) list);
      }
      else
         throw new UnsupportedOperationException();
   }

   public void appendElement(T elem) {
      if (manageChildren && parentComponent != null)
         SwingUtil.addChild(parentComponent, elem);
   }

   public void insertElement(T elem, int ix) {
      if (manageChildren && parentComponent != null)
         SwingUtil.addChild(parentComponent, elem, ix);
   }

   public void removeElement(T elem, int ix) {
      if (manageChildren && parentComponent != null)
         SwingUtil.removeChild(parentComponent, elem);
      // Remove all of the bindings on all of the children when we remove the tag.  ?? Do we need to queue these up and do them later for some reason?
      DynUtil.dispose(elem, true);
   }

   public void removeAllElements() {
      List<T> children = new ArrayList<T>(repeatComponents);
      for (int i = repeatComponents.size() - 1; i >= 0; i--) {
         removeElement(repeatComponents.get(i), i);
      }
      repeatComponents.clear();
   }

   public void moveElement(T elem, int fromIx, int toIx) {
      if (manageChildren && parentComponent != null) {
         SwingUtil.removeChild(parentComponent, elem);
         SwingUtil.addChild(parentComponent, elem, toIx);
      }
   }

   /** Sets the parent widget.  The children can use this to figure out which swing component to add themselves to for dynamically created components */
   public void setParentComponent(java.awt.Component parent) {
      parentComponent = parent;
   }

   public java.awt.Component getParentComponent() {
      return parentComponent;
   }

   public Object[] getChildren() {
      return repeatComponents.toArray();
   }

   public java.awt.Component getLastComponent() {
      int sz = repeatComponents.size();
      if (sz == 0)
         return parentComponent;
      else {
         Object c = repeatComponents.get(sz - 1);
         if (c instanceof java.awt.Component)
            return (java.awt.Component) c;
         else if (c instanceof IChildContainer)
            return ((IChildContainer) c).getLastComponent();
         else
            return null;
      }
   }

   public Object[] getObjChildren(boolean create) {
      return repeatComponents.toArray();
   }

}

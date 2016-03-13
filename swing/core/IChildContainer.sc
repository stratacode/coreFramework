
public interface IChildContainer { 
   /** Sets the parent widget.  The children can use this to figure out which swing component to add themselves to for dynamically created components */
   public void setParentComponent(java.awt.Component parent);

   public java.awt.Component getParentComponent();

   /** Implemented by components that wrap swing children.  This lets you implement groups of swing components or other groups without adding a new swing parent.  Do not confuse this with IObjChildren.getObjChildren - this will collate children and do other processing.  */
   public Object[] getChildren();
}

/**
 * The base class for binding objects which can be the root level binding - i.e. that have
 * a reference to the dstObj, the dstProp, and are initially given the direction.
 */
public abstract class DestinationListener extends AbstractListener implements IBinding {
   Object dstObj;    // The object whose property is set by a forward binding
   Object dstProp;   // The property/binding set by a forward binding.  dstProp == dstObj for nested bindings
   BindingDirection direction;

   public String toString(String operation, boolean displayValue) {
      if (dstProp != null) {
         StringBuilder sb = new StringBuilder();
         sb.append(objectToString(dstObj));
         sb.append('.');
         sb.append(dstProp);
         sb.append(" ");
         sb.append(direction.getOperatorString());
         sb.append(" ");
         return sb.toString();
      }
      return "";
   }

   public String toString() {
      return toString(null);
   }

   public String toString(String op) {
      return toString(op, false);
   }

   public abstract boolean isValid();

   public abstract int refreshBinding();

   protected boolean isValidObject(Object obj) {
      return obj != null && obj != UNSET_VALUE_SENTINEL && obj != PENDING_VALUE_SENTINEL;
   }

   protected boolean isDefinedObject(Object obj) {
      return obj != UNSET_VALUE_SENTINEL && obj != PENDING_VALUE_SENTINEL;
   }

   protected Object getUnsetOrPending(Object val) {
      return val == PENDING_VALUE_SENTINEL ? val : UNSET_VALUE_SENTINEL;
   }

   protected void applyPendingChildValue(Object val, IBinding src) {
   }
}

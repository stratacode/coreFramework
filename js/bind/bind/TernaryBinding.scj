/*
 * Copyright (c) 2009. Jeffrey Vroom
 */

import sc.dyn.DynUtil;

public class TernaryBinding extends AbstractMethodBinding {
   public TernaryBinding(IBinding[] parameterBindings) {
      super(parameterBindings);
   }
   public TernaryBinding(Object dstObject, String dstBinding, IBinding[] parameterBindings, BindingDirection dir) {
      super(dstObject, dstBinding, dstObject, parameterBindings, dir);
   }

   protected void initParams() {
      if (direction.doForward()) {
         boundParams[1].activate(false, null, false);
         boundParams[2].activate(false, null, false);
      }
      super.initParams();
   }

   /** Do propagate the reverse flag down the true/false sl */
   @Override
   boolean propagateReverse(int ix) {
      return ix != 0;
   }

   protected boolean needsMethodObj() {
      return false;
   }

   protected Object invokeMethod(Object obj) {
      Object val = boundParams[0].getPropertyValue(obj);
      if (val == null || val == UNSET_VALUE_SENTINEL) {
         if (Bind.trace)
            System.out.println("Unset condition: " + this);
         return UNSET_VALUE_SENTINEL;
      }
      if (val == PENDING_VALUE_SENTINEL)
         return val;
      Boolean condition = (Boolean) val;
      
      if (condition) {
         if (direction.doForward()) {
            boundParams[1].activate(true, obj, false);
            boundParams[2].activate(false, obj, false);
         }
         return boundParams[1].getPropertyValue(obj);
      }
      else {
         if (direction.doForward()) {
            boundParams[2].activate(true, obj, false);
            boundParams[1].activate(false, obj, false);
         }
         return boundParams[2].getPropertyValue(obj);
      }
   }

   /** Called when reverse bindings fire */
   protected Object invokeReverseMethod(Object obj, Object value) {
      Object val = boundParams[0].getPropertyValue(obj);
      if (val == null || val == UNSET_VALUE_SENTINEL)
         return UNSET_VALUE_SENTINEL;
      if (val == PENDING_VALUE_SENTINEL)
         return val;
      Boolean condition = (Boolean) val;
      IBinding selBoundParam = condition ? boundParams[1] : boundParams[2];

      if (direction == BindingDirection.REVERSE) {
         // For non-invertible reverse only bindings, we do not propagate the value.  We just evaluate the reverse binding
         // This allows an v =: x ? a : b binding where v does not have the same type as a and b.
         // But if you have v =: x ? v1 : v2 you do want to be able to propagate the value to set v1 or v2.
         // TODO: should we also check if the type is compatible here and set it to null in that case?
         if (!selBoundParam.isReversible())
            value = null;
      }

      selBoundParam.applyReverseBinding(obj, value, this);
      return value;
   }

   public String toString(String operation, boolean displayValue) {
      StringBuilder sb = new StringBuilder();
      if (dstObj != dstProp && operation != null) {
         sb.append(operation);
         sb.append(" ");
      }
      sb.append(super.toString(operation, displayValue));
      if (!displayValue || dstObj != dstProp) {
         sb.append("(");
         sb.append(boundParams[0]);
         sb.append(" ? ");
         sb.append(boundParams[1]);
         sb.append(" : ");
         sb.append(boundParams[2]);
         sb.append(")");
      }
      if (dstObj != dstProp && valid && displayValue)
         sb.append(" = " + DynUtil.getInstanceName(boundValue));
      return sb.toString();
   }

   /** When we are invalidated, do not run any children of the ternary. */
   public void invalidateBinding(Object obj, boolean sendEvent, boolean invalidateParams) {
      if (valid && direction.doForward()) {
         boundParams[1].activate(false, obj, false);
         boundParams[2].activate(false, obj, false);
      }
      super.invalidateBinding(obj, sendEvent, invalidateParams);
   }

   public boolean isReversible() {
      return true;
   }
}

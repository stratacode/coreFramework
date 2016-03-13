/*
 * Copyright (c) 2009. Jeffrey Vroom
 */

import sc.dyn.DynUtil;

public class ConditionalBinding extends AbstractMethodBinding {
   String operator;
   public ConditionalBinding(String op, IBinding[] parameterBindings) {
      super(parameterBindings);
      operator = op;
   }
   public ConditionalBinding(Object dstObject, String dstBinding, String op, IBinding[] parameterBindings, BindingDirection dir) {
      super(dstObject, dstBinding, dstObject, parameterBindings, dir);
      operator = op;
   }

   protected boolean needsMethodObj() {
      return false;
   }

   protected Object invokeMethod(Object obj) {
      Object lhsVal = boundParams[0].getPropertyValue(obj);
      Object res = lhsVal;
      paramValues[0] = lhsVal;
      if (lhsVal == PENDING_VALUE_SENTINEL)
         return PENDING_VALUE_SENTINEL;
      if (lhsVal == UNSET_VALUE_SENTINEL)
         return UNSET_VALUE_SENTINEL;

      for (int i = 1; i < boundParams.length; i++) {
         // TODO: do we need to add some form of typing to the binding interface?  Conditional expressions would
         res = DynUtil.evalPreConditionalExpression(operator, lhsVal);
         IBinding boundP = boundParams[i];
         if (res == null) {
            boundP.activate(true, obj, false);
            Object rhsVal = boundParams[i].getPropertyValue(obj);
            paramValues[i] = rhsVal;
            if (rhsVal == PENDING_VALUE_SENTINEL)
               return PENDING_VALUE_SENTINEL;
            if (rhsVal == UNSET_VALUE_SENTINEL)
               return UNSET_VALUE_SENTINEL;

            // propagate their type.  The top-level guy would get the type from the dst property mapper.
            res = DynUtil.evalConditionalExpression(operator, lhsVal, rhsVal);
         }
         else {
            boundP.activate(false, obj, false);
         }
      }
      return res;
   }

   /** Called when reverse bindings fire */
   protected Object invokeReverseMethod(Object obj, Object value) {
      throw new UnsupportedOperationException("Reverse bindings not implemented for conditional expressions");
   }

   @Override
   boolean propagateReverse(int ix) {
      return false;
   }

   public String toString(String operation, boolean displayValue) {
      StringBuilder sb = new StringBuilder();
      if (dstObj != dstProp && operation != null) {
         sb.append(operation);
         sb.append(" ");
      }
      sb.append(super.toString(operation, displayValue));
      if (dstObj != dstProp && displayValue) {
         sb.append(toBindingString(false));
         sb.append(" = ");
      }
      sb.append(toBindingString(displayValue));
      if (activated && valid && displayValue) {
         sb.append(" = ");
         sb.append(DynUtil.getInstanceName(boundValue));
      }
      return sb.toString();
   }

   public StringBuilder toBindingString(boolean displayValue) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < boundParams.length; i++) {
         if (i != 0) {
            sb.append(" ");
            sb.append(operator);
            sb.append(" ");
         }
         if (displayValue)
            sb.append(paramValues == null ? "null" : DynUtil.getInstanceName(paramValues[i]));
         else
            sb.append(boundParams == null ? "null" : boundParams[i]);
      }
      return sb;
   }
}

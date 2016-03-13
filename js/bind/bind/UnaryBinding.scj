/*
 * Copyright (c) 2009. Jeffrey Vroom
 */

import sc.dyn.DynUtil;

public class UnaryBinding extends AbstractMethodBinding {
   String operator;
   public UnaryBinding(String op, IBinding[] parameterBindings) {
      super(parameterBindings);
      operator = op;
   }
   public UnaryBinding(Object dstObject, String dstBinding, String op, IBinding[] parameterBindings, BindingDirection dir) {
      super(dstObject, dstBinding, dstObject, parameterBindings, dir);
      operator = op;
   }

   protected boolean needsMethodObj() {
      return false;
   }

   protected Object invokeMethod(Object obj) {
      Object val = boundParams[0].getPropertyValue(obj);
      if (!isValidObject(val))
          return getUnsetOrPending(val);

      // TODO: do we need to add some form of typing to the binding interface?  Unary expressions would
      // propagate their type.  The top-level guy would get the type from the dst property mapper.
      return DynUtil.evalUnaryExpression(operator, null, val);
   }

   /** Called when reverse bindings fire */
   protected Object invokeReverseMethod(Object obj, Object value) {
      if (!isValidObject(value))
         return getUnsetOrPending(value);
      return DynUtil.evalInverseUnaryExpression(operator, null, value);
   }

   @Override
   boolean propagateReverse(int ix) {
      return true;
   }

   public String toString(String operation, boolean displayValue) {
      StringBuilder sb = new StringBuilder();
      if (dstObj != dstProp && operation != null) {
         sb.append(operation);
         sb.append(" ");
      }
      sb.append(super.toString(operation, displayValue));
      sb.append(operator);
      if (displayValue && paramValues[0] != null) {
         if (dstObj != dstProp) {
            sb.append(boundParams[0]);
            sb.append(" = ");
         }
         sb.append(paramValues[0]);
      }
      else
         sb.append(boundParams[0]);
      if (dstObj != dstProp && valid && displayValue)
         sb.append(" = " + boundValue);
      return sb.toString();
   }
}

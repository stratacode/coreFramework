/*
 * Copyright (c) 2009. Jeffrey Vroom
 */

import sc.dyn.DynUtil;

public class CastBinding extends AbstractMethodBinding {
   Object castToClass;
   Object lastType;
   public CastBinding(Object theClass, IBinding parameterBindings) {
      super(new IBinding[] {parameterBindings});
      castToClass = theClass;
   }
   public CastBinding(Object dstObject, String dstBinding, Object theClass, IBinding parameterBindings, BindingDirection dir) {
      super(dstObject, dstBinding, dstObject, new IBinding[] {parameterBindings}, dir);
      castToClass = theClass;
   }

   protected boolean needsMethodObj() {
      return false;
   }

   protected Object invokeMethod(Object obj) {
      Object val = boundParams[0].getPropertyValue(obj);
      if (val == UNSET_VALUE_SENTINEL)
         return UNSET_VALUE_SENTINEL;
      if (val == PENDING_VALUE_SENTINEL)
         return PENDING_VALUE_SENTINEL;
      // TODO: do we need to add some form of typing to the binding interface?  Cast expressions would
      // propagate their type.  The top-level guy would get the type from the dst property mapper.
      if (val != null)
         lastType = val.getClass();
      paramValues[0] = DynUtil.evalCast(castToClass, val);
      return paramValues[0];
   }

   /** Called when reverse bindings fire */
   protected Object invokeReverseMethod(Object obj, Object value) {
      if (lastType != null && isDefinedObject(value))
         paramValues[0] = value = DynUtil.evalCast(lastType, value);

      // For number types, do we need to figure out the original type and go backwards?
      boundParams[0].applyReverseBinding(obj, value, this);
      return value;
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
      sb.append("(");
      sb.append(castToClass);
      sb.append(") ");
      if (dstObj != dstProp && displayValue) {
         sb.append(Bind.arrayToString(boundParams));
         sb.append(" = ");
      }
      sb.append(toBindingString(true));
      if (valid && displayValue && dstObj != dstProp && !(boundParams[0] instanceof VariableBinding)) {
         sb.append(" = ");
         sb.append(DynUtil.getInstanceName(boundValue));
      }
      return sb.toString();
   }

   protected boolean useParens() {
      return false;
   }
}

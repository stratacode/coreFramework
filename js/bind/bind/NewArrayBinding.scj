import sc.dyn.DynUtil;
import sc.type.PTypeUtil;

import java.util.ArrayList;
import java.util.List;

public class NewArrayBinding extends AbstractMethodBinding {
   Object compClass;

   public NewArrayBinding(Object compClass, IBinding[] parameterBindings) {
      super(parameterBindings);
      this.compClass = compClass;
   }
   public NewArrayBinding(Object dstObject, String dstBinding, Object compClass, IBinding[] parameterBindings, BindingDirection dir) {
      super(dstObject, dstBinding, dstObject, parameterBindings, dir);
      this.compClass = compClass;
   }

   protected Object invokeMethod(Object obj) {
      boolean valid = true;
      for (int i = 0; i < paramValues.length; i++) {
         paramValues[i] = boundParams[i].getPropertyValue(obj);
         if (paramValues[i] == UNSET_VALUE_SENTINEL)
            valid = false;
      }

      if (valid) {
         Class cl = DynUtil.getCompiledClass(compClass);
         if (DynUtil.isAssignableFrom(List.class, cl)) {
            // TODO: need to get concrete type from the class, find a constructor for Object[] and use that here?
            // Or should the constructor be passed as an optional parameter
            List l = new ArrayList(paramValues.length);
            for (int i = 0; i < paramValues.length; i++)
               l.add(paramValues[i]);
            return l;
         }
         else {
            Object arr = PTypeUtil.newArray(cl, paramValues.length);
            for (int i = 0; i < paramValues.length; i++)
               PTypeUtil.setArrayElement(arr, i, paramValues[i]);
            return arr;
         }
      }
      else
         return UNSET_VALUE_SENTINEL;
   }

   protected boolean needsMethodObj() {
      return false;
   }

   /** Called when reverse bindings fire */
   protected Object invokeReverseMethod(Object obj, Object value) {
      if (!direction.doForward())
         return invokeMethod(obj);
      System.err.println("*** reverse binding not supported on new expressions");
      return null;
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
      sb.append("new ");
      sb.append(compClass);
      sb.append("[] {");
      if (displayValue && dstObj != dstProp) {
         sb.append(Bind.arrayToString(boundParams));
         sb.append(") = ");
      }
      if (displayValue)
         sb.append(DynUtil.arrayToInstanceName(paramValues));
      else
         sb.append(Bind.arrayToString(boundParams));
      sb.append(")");
      return sb.toString();
   }
}

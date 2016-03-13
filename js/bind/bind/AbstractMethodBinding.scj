import sc.dyn.DynUtil;

public abstract class AbstractMethodBinding extends DestinationListener {
   Object methObj; // Usually equal to dstObj but can be different with nested types
   IBinding[] boundParams;
   Object[] paramValues;
   Object boundValue;
   boolean constant;
   boolean valid = false;
   boolean methObjSet = false;

   public boolean isValid() {
      return valid;
   }

   AbstractMethodBinding(IBinding[] params) {
      boundParams = params;
   }

   public AbstractMethodBinding(Object dstObject, String dstBinding, Object methodObject, IBinding[] parameterBindings, BindingDirection dir) {
      this(parameterBindings);
      dstObj = dstObject;
      setMethObj(methodObject);
      methObjSet = methodObject != null;
      dstProp = dstBinding;
      direction = dir;
   }

   abstract protected Object invokeMethod(Object obj);

   abstract protected Object invokeReverseMethod(Object obj, Object value);

   protected void setMethObj(Object newMethObj) {
      methObj = newMethObj;
   }

   /** Called by the parent when this is a hierarchical binding - i.e. one expression in another */
   public void setBindingParent(IBinding parent, BindingDirection dir) {
      dstProp = null;
      dstObj = parent;
      direction = dir;

      initParams();
   }

   protected void initParams() {
      boolean allConst = true;
      paramValues = new Object[boundParams.length];
      int i = 0;
      for (IBinding param:boundParams) {
         if (!param.isConstant())
            allConst = false;
         BindingDirection propBD;
         if (direction.doReverse() && !propagateReverse(i))
            propBD = direction.doForward() ? BindingDirection.FORWARD : BindingDirection.NONE;
         else
            propBD = direction;
         param.setBindingParent(this, propBD);
         i++;
      }
      constant = allConst;
   }

   abstract boolean propagateReverse(int ix);

   /** Called only for top level bindings */
   public Object initializeBinding() {
      assert null != dstProp; // This should only be called for a top-level binding

      initParams();

      Object result = null;
      if (direction.doForward()) {
         if (!activated)
            result = null;
         else {
            result = invokeMethod(methObj);
            if (isDefinedObject(result))
               valid = true;
         }
         boundValue = result;
      }
      if (direction.doReverse()) {
         // TODO: need flag for "read-only" bindings - throw if it is a read-only
         if (dstProp == null)
            ((IBinding) dstObj).addBindingListener(dstObj, this, VALUE_CHANGED_MASK);
         else
            Bind.addListener(dstObj, dstProp, this, VALUE_CHANGED_MASK);

         // This is a bit of a hack.  For reverse only bindings, we should not be changing the value of the property
         // at all.  Because the binding replaces the assignment expression, it's awkward to rewrite the code so the
         // property assignment never happens.  Instead, we'll just change it to the same value.
         // This also lets us register the IChangeable hook and keep it up to date in the value change part
         if (!direction.doForward()) {
            if (dstProp == null)
               boundValue = ((IBinding) dstObj).getPropertyValue(dstObj);
            else
               boundValue = PBindUtil.getPropertyValue(dstObj, dstProp);
            if (useReverseListener()) {
               if (boundValue instanceof IChangeable)
                  Bind.addListener(boundValue, null, this, VALUE_CHANGED_MASK);
            }
            result = boundValue;
         }
      }
      //if (trace)
      //   System.out.println("Init: " + toString());
      if (!isDefinedObject(result))
         result = null;
      return result;
   }

   private void reactivate(Object obj) {
      if (direction.doForward()) {
         if (!methObjSet)
            setMethObj(obj);

         if (methObj == null)
            boundValue = null;
         else
            boundValue = invokeMethod(methObj);
      }
      else if (direction.doReverse()) {
         Object newValue;
         if (dstProp == null)
            newValue = ((IBinding) dstObj).getPropertyValue(dstObj);
         else
            newValue = PBindUtil.getPropertyValue(dstObj, dstProp);
         if (!equalObjects(newValue, boundValue)) {
            if (useReverseListener()) {
               if (boundValue instanceof IChangeable)
                  Bind.removeListener(boundValue, null, this, VALUE_CHANGED_MASK);
               if (newValue instanceof IChangeable)
                  Bind.addListener(newValue, null, this, VALUE_CHANGED_MASK);
            }
            boundValue = newValue;
         }
      }
   }

   boolean useReverseListener() {
      return direction.doReverse() && !direction.doForward() && null != dstProp;
   }

   public Object getPropertyValue(Object object) {
      // Do not cache results when it's a reverse only binding.  We're not listening on our parameters in
      // that case so we can't rely on being notified.
      if (!valid || !direction.doForward()) {
         if (!activated) {
            boundValue = null;
         }
         else {
            boundValue = invokeMethod(object);
            if (isDefinedObject(boundValue))
               valid = true;
         }
         // If we were set with an explicit method object, always use that one.  Otherwise, we'll use
         // the one from the previous binding in the chain.
         if (!methObjSet)
            setMethObj(object);
      }
      return boundValue;
   }

   /** Implemented only for child property bindings */
   public void addBindingListener(Object eventObject, IListener listener, int event) {
      /*
      if (dstObj == dstProp) {
         for (IBinding param:boundParams)
           param.addBindingListener(eventObject, this, event);
      }
      */
   }

   /** Abstract method objects don't in general do this.  Their listeners are implicitly added via the
    *  bindings list in the parameters.  That gets set up during init and torn down in "removeListener". */
   public void removeBindingListener(Object eventObject, IListener listener, int event) {
      //if (dstObj == dstProp) {
      //   removeListener();
            //param.removeBindingListener(eventObject, this, event);
      //}
   }

   public void invalidateBinding(Object object, boolean sendEvent, boolean invalidateParams) {
      valid = false;
      // This option walks down the chain and is used when we call 'refreshBinding' - we need invalidate all of the parameters in the call chain.
      if (invalidateParams) {
         for (IBinding param:boundParams) {
            param.invalidateBinding(object, sendEvent, true);
         }
      }
      if (dstObj instanceof IBinding)
         ((IBinding) dstObj).invalidateBinding(dstObj, sendEvent, false);
      else if (dstProp instanceof String) {
         if (sendEvent)
            Bind.sendEvent(VALUE_CHANGED, dstObj, (String) dstProp);
      }
   }

   /** Override in subclasses that do not use the methObj so we can tell when the method just could not be resolved */
   protected boolean needsMethodObj() {
      return true;
   }

   boolean removed = false;
   public void removeListener() {
      if (removed) {
         System.err.println("*** removing binding twice");
         return;
      }
      removed = true;
      if (direction.doReverse() && dstProp != null)
         Bind.removeListener(dstObj, dstProp, this, VALUE_CHANGED_MASK);

      // For the special reverse-only bindings we add the listener onto the IChangeable values
      if (direction.doReverse() && !direction.doForward()) {
         if (useReverseListener()) {
            if (boundValue instanceof IChangeable)
               Bind.removeListener(boundValue, null, this, VALUE_CHANGED_MASK);
         }
      }
      for (IBinding param:boundParams)
         param.removeListener();
   }

   public void parentBindingChanged() {
      valid = false;
   }

   public boolean applyBinding(Object obj, Object value, IBinding src) {
      return applyBinding(obj, value, src, false);
   }

   public boolean applyBinding(Object obj, Object value, IBinding src, boolean refresh) {
      Object newBoundValue = null;

      try {
         // When we don't have a method object and there is no current value, we can't apply the method
         // or it will get an RTE.
         if (obj == null && (needsMethodObj() && methObj == null))
            newBoundValue = null;
         else
            newBoundValue = activated ? invokeMethod(obj == null ? methObj : obj) : null;
      }
      catch (Throwable exc) {
         System.err.println("*** Error applying binding: " + this + " with value: " + value + " :" + exc.toString());
         exc.printStackTrace();
      }

      boolean valueChanged = !equalObjects(newBoundValue, boundValue);
      if (refresh || !valid || valueChanged) {
         if (refresh && !valueChanged)
            return false;

         boundValue = newBoundValue;
         if (activated && isDefinedObject(boundValue))
            valid = true;

         if (null != dstProp) {
            if (newBoundValue == UNSET_VALUE_SENTINEL)
               newBoundValue = null;

            //if (trace)
            //   System.out.println(toString("<" + direction.getOperatorString()));
         }

         if (newBoundValue != PENDING_VALUE_SENTINEL) {
            applyChangedValue(newBoundValue);
            return true;
         }
      }
      return false;
   }

   protected void applyPendingValue(Object pendingResult) {
      boundValue = pendingResult;
      if (activated)
         valid = true;
      if (dstProp == null) {
         if (dstObj instanceof DestinationListener)
            ((DestinationListener) dstObj).applyPendingChildValue(pendingResult, this);
         else
            System.err.println("*** Unhandled case in applyPendingValue");
      }
      else if (direction.doForward()) {
         applyChangedValue(boundValue);
      }
   }

   protected void applyChangedValue(Object newBoundValue) {
      Bind.applyBinding(dstProp == null ? null : dstObj, dstProp == null ? dstObj : dstProp, newBoundValue, this);
   }

   protected boolean equalObjects(Object v1, Object v2) {
      return DynUtil.equalObjects(v1, v2);
   }

   public void applyReverseBinding(Object obj, Object value, Object src) {
      boolean endLogIndent = false;
      try {
         if (Bind.trace)
            endLogIndent = Bind.logBindingMessage("reverse", this, obj, value, src);

         // Have to set boundValue here, not after invokeReverseMethod because that can in turn retrigger lots of stuff like
         // the removal of the listener etc.
         boundValue = value;
         invokeReverseMethod(obj, value); // used to be methObj here but we need the accurate bound value here, not the cached one from last time
      }
      finally {
         if (endLogIndent)
            Bind.endPropMessage();
      }
      //if (trace && dstProp != dstObj)
      //   System.out.println(toString(direction.getOperatorString() + ">"));
   }

   public boolean valueValidated(Object srcObject, Object srcProp, Object eventDetail, boolean apply) {
      // Added the listener to methObj to detect any events which should invalidate the listeners on the method object
      if (direction.doForward() && !direction.doReverse()) {
         if (srcProp == null) {
            if (apply)
               applyBinding(null, null, null);
         }
         return true;
      }
      // If either the default event on the IChangeable, or it's the dest property changing
      else if (direction.doReverse()) {
         if ((dstObj == srcObject && dstProp != null && PBindUtil.equalProps(dstProp,srcProp))) {
            Object currentValue = PBindUtil.getPropertyValue(srcObject, dstProp);
            if (!equalObjects(currentValue, boundValue)) {

               if (useReverseListener()) {
                  // We also listen for any events this object sends on itself if it is marked with IChangeable
                  if (boundValue instanceof IChangeable)
                     Bind.removeListener(boundValue, null, this, VALUE_CHANGED_MASK);
                  if (currentValue instanceof IChangeable)
                     Bind.addListener(currentValue, null, this, VALUE_CHANGED_MASK);
               }
               boundValue = currentValue;

               if (apply)
                  applyReverseBinding(methObj, currentValue, this);
               return true;
            }
         }
         // We add the default value listener both on the value of the dstObj for a top-level binding 
         // (e.g. editorModel =: (x > y ? doX() : doY()) as well as to the methObj in:
         // value :=: methObj.get(x).   In this later case the null event occurs on teh srcObj.  Doing the reverse
         // binding will infinite loop us.
         else if (srcProp == null && srcObject != methObj) {
            if (apply)
               applyReverseBinding(methObj, boundValue, this);
            return true;
         }
         return false;
      }
      else
         throw new UnsupportedOperationException();
   }

   public boolean valueInvalidated(Object srcObject, Object srcProp, Object eventDetail, boolean apply) {
      // Added the listener to methObj to detect any events which should invalidate the listeners on the method object
      if (direction.doForward() && !direction.doReverse()) {
         if (srcProp == null) {
            valid = false;
            invalidateBinding(null, false, false);
         }
         return true;
      }
      else if (direction.doReverse()) {
         if ((dstObj == srcObject && dstProp != null && PBindUtil.equalProps(dstProp,srcProp))) {
            Object currentValue = PBindUtil.getPropertyValue(srcObject, srcProp);
            if (!equalObjects(currentValue, boundValue)) {
               return true;
            }
         }
         else if (srcProp == null) {
            return true;
         }
         return false;
      }
      else
         throw new UnsupportedOperationException();
   }

   public boolean isConstant() {
      return constant;
   }

   public void activate(boolean state, Object obj, boolean chained) {
      if (state == activated)
         return;
      super.activate(state, obj, chained);
      for (IBinding bp:boundParams)
         bp.activate(state, obj, true);

      if (!state)
         valid = false;
      else if (!valid && !chained)
         reactivate(obj);
   }

   public IBinding[] getBoundParams() {
      return boundParams;
   }

   protected boolean useParens() {
      return true;
   }

   public StringBuilder toBindingString(boolean displayValue) {
      StringBuilder sb = new StringBuilder();
      if (useParens())
         sb.append("(");
      if (displayValue) {
         for (int i = 0; i < paramValues.length; i++) {
            if (i != 0)
               sb.append(",");
            if (boundParams[i] instanceof ArithmeticBinding)
               sb.append(((ArithmeticBinding) boundParams[i]).toBindingString(true));
            else
               sb.append(DynUtil.getInstanceName(paramValues[i]));
         }
         //sb.append(DynUtil.arrayToInstanceName(paramValues));
      }
      else
         sb.append(Bind.arrayToString(boundParams));
      if (useParens())
         sb.append(")");
      return sb;
   }

   protected boolean isRefreshDisabled() {
      return false;
   }

   public int refreshBinding() {
      if (!activated)
         return 0;

      if (direction.doForward() && !direction.doReverse() && !isRefreshDisabled()) {
         invalidateBinding(null, false, true);
         if (applyBinding(null, null, null, true))
            return 1;
         return 0;
      }
      return 0;
   }

   // Methods are not receiving values from reverse-only bindings.
   public boolean isReversible() {
      return false;
   }

   // A nested method in a has a pending result.
   protected void applyPendingChildValue(Object pendingResult, IBinding src) {
      for (int i = 0; i < paramValues.length; i++) {
         if (boundParams[i] == src) {
            // assert paramValues[i] == PENDING_VALUE_SENTINEL
            paramValues[i] = pendingResult;
            if (!hasPendingParams())
               applyBinding(null, null, null);
            break;
         }
      }
   }

   protected boolean hasPendingParams() {
      for (int i = 0; i < paramValues.length; i++) {
         if (paramValues[i] == PENDING_VALUE_SENTINEL)
            return true;
      }
      return false;
   }
}

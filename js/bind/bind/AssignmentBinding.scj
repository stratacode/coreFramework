import sc.dyn.DynUtil;

public class AssignmentBinding extends DestinationListener {
   VariableBinding lhsBinding;
   IBinding rhsBinding;
   volatile boolean valid = false;

   Object srcObj;
   Object boundValue;
   Object lastTriggeredValue;

   public AssignmentBinding(Object srcObject, VariableBinding lhsBinding, IBinding rhsBinding) {
      srcObj = srcObject;
      this.lhsBinding = lhsBinding;
      this.rhsBinding = rhsBinding;
   }

   public AssignmentBinding(Object srcObject, VariableBinding lhsBinding, Object rhsValue) {
      srcObj = srcObject;
      this.lhsBinding = lhsBinding;
      this.rhsBinding = rhsValue instanceof IBinding ? (IBinding) rhsValue : new ConstantBinding(rhsValue);
   }

   public AssignmentBinding(Object dstObject, Object dstProperty, Object srcObject, VariableBinding lhsBinding, IBinding rhsBinding,
                       BindingDirection bindingDirection) {
      this(srcObject, lhsBinding, rhsBinding);
      dstObj = dstObject;
      dstProp = dstProperty;
      direction = bindingDirection;
   }

   public AssignmentBinding(Object dstObject, Object dstProperty, Object srcObject, VariableBinding lhsBinding, Object rhsValue,
                            BindingDirection bindingDirection) {
      this(srcObject, lhsBinding, rhsValue);
      dstObj = dstObject;
      dstProp = dstProperty;
      direction = bindingDirection;
   }

   @Override
   public boolean isValid() {
      // Right now, the only non DestinationListener for the RHS is a constant
      if (rhsBinding.isConstant())
         return true;
      return ((DestinationListener) rhsBinding).isValid();
   }

   public Object getPropertyValue(Object parent) {
      return rhsBinding.getPropertyValue(parent);
   }

   public void addBindingListener(Object eventObject, IListener listener, int event) {
      if (null == dstProp && isValidObject(eventObject)) {
           rhsBinding.addBindingListener(eventObject, this, event);
      }
   }

   public void removeBindingListener(Object eventObject, IListener listener, int event) {
      if (null == dstProp && isValidObject(eventObject)) {
         rhsBinding.removeBindingListener(eventObject, this, event);
      }
   }

   // Should only be used with ON_DEMAND sync'ing which only works when dstObj is IBindable
   private void invalidate(boolean sendEvent) {
      if (dstProp == null)
         ((IBinding) dstObj).invalidateBinding(dstObj, sendEvent, false);
      else
         Bind.sendEvent(VALUE_CHANGED_MASK, dstObj, (String) dstProp);
      valid = false;
   }

   public void invalidateBinding(Object object, boolean sendEvent, boolean includeParams) {
      invalidate(sendEvent);
   }

   public boolean applyBinding(Object obj, Object value, IBinding src) {
      if (!DynUtil.equalObjects(boundValue, value)) {
         boundValue = value;
         applyBinding();
         return true;
      }
      return false;
   }

   void applyBinding() {
      // Get this before marking it valid
      Object newValue = getBoundValue();

      applyBindingWithValue(newValue);
   }

   void applyBindingWithValue(Object newValue) {
      if (activated)
         valid = true;

      if (null != dstProp) {

         // Don't set the top-level property with the sentinel, do pass it up to nested bindings though
         // so they know their value is UNSET rather than null.
         if (newValue == UNSET_VALUE_SENTINEL)
            newValue = null;

         //if (trace)
         //   System.out.println(toString("<="));
      }

      if (dstProp == null)
         ((IBinding) dstObj).applyBinding(null == dstProp ? null : dstObj, newValue, this);
      else if (newValue != PENDING_VALUE_SENTINEL)
         PBindUtil.setPropertyValue(dstObj, dstProp, newValue); 
   }

   public void applyReverseBinding(Object obj, Object value, Object src) {
      // This is the =: case which is different.
      if (direction.doReverse() && !direction.doForward())
         doAssignment();
      else {
         rhsBinding.applyReverseBinding(obj, value, src);
         doAssignment();
      }
   }

   protected void applyReverseBinding() {
      doAssignment();
      // Any other cases here that need to be propagated?
      if (rhsBinding instanceof AssignmentBinding)
         ((AssignmentBinding) rhsBinding).applyReverseBinding();
   }

   /** Called by the parent when this is a hierarchical binding - i.e. one expression in another */
   public void setBindingParent(IBinding parent, BindingDirection dir) {
      dstProp = null;
      dstObj = parent;
      direction = dir;
      initBinding();
      addListeners();
   }

   public boolean isConstant() {
      return false;
   }

   /** Called to initialize and retrieve the value of a top-level binding */
   public Object initializeBinding() {
      assert null != dstProp;

      initBinding();
      addListeners();
      if (direction.doForward()) {
         Object result = getBoundValue();
         //if (trace) {
         //   System.out.println("Init: " + this);
         //}
         if (!isValidObject(result))
            result = null;
         return result;
      }
      else
         return null;
   }

   protected Object getBoundValue() {
      if (direction.doForward())
         return boundValue;
      else // reverse bindings are not cached - no listeners so we eval them each time
         return rhsBinding.getPropertyValue(srcObj);
   }

   protected void initBinding() {
      lhsBinding.isAssignment = true;
      lhsBinding.setBindingParent(this, direction);
      if (rhsBinding == null)
         System.out.println("*** RHS of Assignment binding did not resolve");
      rhsBinding.setBindingParent(this, direction);
      if (direction.doForward()) {
         boundValue = rhsBinding.getPropertyValue(srcObj);
         doAssignment();
      }
      else if (direction.doReverse() && null != dstProp) {
         // Only fire after this value has changed
         lastTriggeredValue = PBindUtil.getPropertyValue(dstObj, dstProp);
      }
      if (activated)
         valid = true;
   }

   protected void reactivate(Object obj) {
      if (direction.doForward()) {
         Object bindingParent = srcObj;

         if (isValidObject(bindingParent)) {
            boundValue = rhsBinding.getPropertyValue(bindingParent);
            doAssignment();
         }
      }
      valid = true;
   }

   private void doAssignment() {
      Object bv = getBoundValue();
      if (isDefinedObject(bv)) {
         // Sets the value for lhsBinding's property to the boundValue specified.
         lhsBinding.applyReverseBinding(srcObj, bv, this);
      }
   }

   private void addListeners() {
      // We always add the forward listeners so that we do not have to re-evaluate everything on demand
      if (srcObj != null) {
         if (direction.doForward()) {
            rhsBinding.addBindingListener(srcObj, this, VALUE_CHANGED_MASK);
            if (srcObj instanceof IChangeable) {
               Bind.addListener(srcObj, null, this, VALUE_CHANGED_MASK);
            }

            // If the value wants to be notified of individual events, always listen on it
            if (boundValue instanceof IChangeable) {
               Bind.addListener(boundValue, null, this, VALUE_CHANGED_MASK);
            }
         }
      }
      if (direction.doReverse() && null != dstProp) {
         Bind.addListener(dstObj, dstProp, this, VALUE_CHANGED_MASK);
      }
   }

   public void removeListener() {
      if (direction.doForward()) {
         if (srcObj != null) {
            rhsBinding.removeBindingListener(srcObj, this, VALUE_CHANGED_MASK);
            if (srcObj instanceof IChangeable) {
               Bind.removeListener(srcObj, null, this, VALUE_CHANGED_MASK);
            }
         }
         if (boundValue instanceof IChangeable) {
            Bind.removeListener(boundValue, null, this, VALUE_CHANGED_MASK);
         }
         PBindUtil.removeStatefulListener(rhsBinding);
      }
      if (direction.doReverse() && dstProp != null) {
         Bind.removeListener(dstObj, dstProp, this, VALUE_CHANGED_MASK);
      }
   }

   public void valueRequested(Object obj, Object prop) {
      if (sync == SyncType.ON_DEMAND) {
         // If the binding is out of date and the value requested is the right one.
         if (!valid) {
            synchronized (this) {
               if (!valid) {
                  if (obj == dstObj && prop == dstProp && direction.doForward()) {
                     applyBinding();
                  }
               }
            }
         }
      }
   }

   public boolean valueValidated(Object srcObject, Object srcProp, Object eventDetail, boolean apply) {
      if (dstObj == srcObject && PBindUtil.equalProps(dstProp, srcProp)) {
         if (direction.doReverse()) {
            if (sync != SyncType.ON_DEMAND) {
               if (apply) {
                  applyReverseBinding();
                  lastTriggeredValue = eventDetail;
               }
            }
         }
         return true;
      }

      boolean changed = false;
      int i;

      if (direction.doForward()) {
         // An event occurred on the value itself for IChangeable objects
         // In this case, we need to mark all dstObj bindings as changed since the physical object may not have changed
         // instead, the value of the internal object changed.  equalObjs will return true.  We need valid=false for
         // all parent bindings.
         if (srcProp == null) {
            if (apply)
               applyBinding();
            return true;
         }

         Object newValue = rhsBinding.getPropertyValue(srcObj);
         if (!DynUtil.equalObjects(newValue, boundValue)) {
            changed = true;
            if (apply)
               applyBinding();
         }
      }
      return changed;
   }

   public boolean valueInvalidated(Object srcObject, Object srcProp, Object eventDetail, boolean apply) {
      if (dstObj == srcObject && PBindUtil.equalProps(dstProp, srcProp)) {
         if (direction.doReverse()) {
            if (sync == SyncType.ON_DEMAND) {
               invalidate(true);
            }
         }
         return true;
      }

      boolean changed = false;
      int i;

      if (direction.doForward()) {
         // An event occurred on the value itself for IChangeable objects
         // In this case, we need to mark all dstObj bindings as changed since the physical object may not have changed
         // instead, the value of the internal object changed.  equalObjs will return true.  We need valid=false for
         // all parent bindings.
         if (srcProp == null) {
            // If we are applying (and thus not already invalidating) first invalidate the parent binding
            if (apply)
               invalidateBinding(null, false, false);
            // Then apply it
            bindingInvalidated(apply);
            return true;
         }

         Object newValue = rhsBinding.getPropertyValue(srcObj);
         if (!DynUtil.equalObjects(newValue, boundValue))
            bindingInvalidated(apply);
      }
      return changed;
   }

   protected void bindingInvalidated(boolean apply) {
      if (sync == SyncType.ON_DEMAND) {
         invalidate(true);
      }
      else {
         invalidateBinding(null, false, false);
      }
   }

   public String toString(String operation, boolean displayValue) {
      StringBuilder sb = new StringBuilder();
      if (null != dstProp && operation != null) {
         sb.append(operation);
         sb.append(" ");
      }
      sb.append(super.toString(operation, displayValue));
      if (displayValue) {
         if (null != dstProp) {
            sb.append(toBindingString());
            sb.append(" ");
         }
         if (direction != null && direction.doForward()) {
            if (null != dstProp) {
               sb.append("= ");
            }
            if (boundValue != null)
               sb.append(DynUtil.getInstanceName(boundValue));
         }
      }
      else {
         sb.append(toBindingString());
      }
      return sb.toString();
   }

   private StringBuilder toBindingString() {
      StringBuilder sb = new StringBuilder();
      if (lhsBinding == null || rhsBinding == null)
         sb.append("<not initialized>");
      else {
         // Indicate that these bindings are not on properties from the "this" object.
         // they could have come from any object reference or a parent of this... not sure
         // how we get from the instance back to its type name (if any)
         if (srcObj != dstObj) {
            sb.append(DynUtil.getInstanceName(srcObj));
            sb.append(".");
         }
         sb.append(lhsBinding.toString());
         sb.append(" = ");
         sb.append(rhsBinding.toString());
      }
      return sb;
   }

   public void activate(boolean state, Object obj, boolean chained) {
      if (state == activated)
         return;
      super.activate(state, obj, chained);
      lhsBinding.activate(state, obj, true);
      rhsBinding.activate(state, obj, true);

      if (state) {
         if (!valid && !chained)
            reactivate(obj);
      }
      else // TODO: required because bindings when deactivated do not always deliver events.  Could be fixed if they invalidate the parent during the re-activation process when their value is changed.  The goal being that if you switch back and forth you do not have to recache the world.  See SelectorBinding as well
         valid = false;
   }

   public int refreshBinding() {
      if (direction.doReverse() && !direction.doForward() && null != dstProp) {
         Object newValue = PBindUtil.getPropertyValue(dstObj, dstProp);
         if (!DynUtil.equalObjects(newValue, lastTriggeredValue)) {
            lastTriggeredValue = newValue;
            doAssignment();
            return 1;
         }
         return 0;
      }
      else
         System.err.println("*** not refreshing assignment binding");
      return 0;
   }

   public boolean isReversible() {
      return false;
   }

   // Called when our expression's value has a result from a remote method.  This will happen when a previous apply binding call
   // was skipped due to a pending value.
   protected void applyPendingChildValue(Object val, IBinding src) {
      boundValue = val;
      lhsBinding.applyReverseBinding(srcObj, val, this);
   }
}

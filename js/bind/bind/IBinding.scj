/*
 * Copyright (c) 2009. Jeffrey Vroom
 */

public interface IBinding {
   /** Retrieves the current value of the binding given the current object. */
   Object getPropertyValue(Object parent);

   void addBindingListener(Object eventObject, IListener listener, int event);

   void removeBindingListener(Object eventObject, IListener listener, int event);

   void invalidateBinding(Object object, boolean sendEvent, boolean invalidateParams);

   boolean applyBinding(Object obj, Object value, IBinding src);

   Object performCast(Object value);

   void applyReverseBinding(Object obj, Object value, Object src);

   void removeListener();

   Object initializeBinding();

   void setBindingParent(IBinding parent, BindingDirection dir);

   boolean isConstant();

   boolean isReversible();

   /**
    * Called from the VariableBinding when the parent's value has changed.  E.g. for "a.b",
    * b.parentBindingChanged() is called when we detect changes on 'a'.
    */
   void parentBindingChanged();

   /** Called to deactive/re-activate a child binding for a condition or ternary expression */
   void activate(boolean state, Object obj, boolean chained);
}

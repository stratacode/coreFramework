import sc.js.JSSettings;

/**
 * This interface is implemented by the VariableBinding, MethodBinding, etc. objects.  The event dispatch mechanism
 * uses it to communicate change event information to the bindings.  The bindings also provide state that controls
 * how events are queued, and removed.
 */
@JSSettings(prefixAlias="sc_")
public interface IListener {
   public enum SyncType {
      IMMEDIATE, ON_DEMAND, QUEUED
   }

   static final int VALUE_INVALIDATED = 1 << 0;
   static final int VALUE_REQUESTED = 1 << 1;
   static final int VALUE_VALIDATED = 1 << 3;
   static final int ARRAY_ELEMENT_INVALIDATED = 1 << 4;
   static final int ARRAY_ELEMENT_VALIDATED = 1 << 5;
   static final int LISTENER_ADDED = 1 << 6;  // When a new listener is added on some property of the object

   static final int ARRAY_ELEMENT_CHANGED = ARRAY_ELEMENT_INVALIDATED | ARRAY_ELEMENT_VALIDATED;
   static final int VALUE_CHANGED = VALUE_INVALIDATED | VALUE_VALIDATED;

   static final int VALUE_CHANGED_MASK = VALUE_CHANGED | ARRAY_ELEMENT_CHANGED;

   /** Stored internally to mark the case where we were not able to evaluate a given value */
   static String UNSET_VALUE_SENTINEL = "<unset>";

   /** Stored to mark the case where we've invoked an asynchronous call to retrieve the value.  We don't fire the binding but mark it pending, to fire when the response comes in. */
   static String PENDING_VALUE_SENTINEL = "<pending>";

   void valueRequested(Object obj, Object prop);
   /**
    * Called to notify the listener of a VALUE_CHANGED event on the property srcProp.
    * The eventDetail may include the new value.   Typically the data binding system works by getting the property
    * value at the time a binding is evaluated, not using the value which has been propagated since it may have changed
    * since the event delivery started.
    * <p>
    * This method returns true if the binding detected that the value had changed.  False if it is detects the binding
    * did not change.  Since most listeners cache the old value, they can tell whether we need to keep processing this
    * change.  If you do not cache the old value, just return "true".
    */
   boolean valueChanged(Object srcObj, Object srcProp, Object eventDetail, boolean apply);

   boolean valueInvalidated(Object srcObj, Object srcProp, Object eventDetail, boolean apply);
   boolean valueValidated(Object srcObj, Object srcProp, Object eventDetail, boolean apply);

   boolean arrayElementChanged(Object srcObj, Object srcProp, Object dims, boolean apply);
   boolean arrayElementInvalidated(Object srcObj, Object srcProp, Object dims, boolean apply);
   boolean arrayElementValidated(Object srcObj, Object srcProp, Object dims, boolean apply);

   boolean listenerAdded(Object srcObj, Object srcProp, Object listener, int eventMask, int priority);

   // Returns the sync mode object.
   SyncType getSync();

   float getPriority();
}

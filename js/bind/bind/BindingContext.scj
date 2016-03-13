import sc.type.PTypeUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * The BindingContext maintains event queues and dispatches events.  It could be stored in thread local
 * or not depending upon the binding manager.
 * <p>
 * Event dispatching can be immediate, or queued into a BindingContext.  With immediate dispatching, the listeners are
 * executed in the order in which they were added with no BindingContext necessary.  The listener controls the behavior through its
 * sync property, or the code to send an event in BindingManager.
 * <p>
 * For UI toolkits that are single-threaded, you can use one BindingContext to serialize all event streams.
 * <p>
 * In multi-threaded servers, you typically have the binding context stored in some manager object local to the
 * session.
 * <p>
 * Transactional memory (to be implemented via scopes) will help improve the applicability of data binding even
 * for shared objects.
 * <p>
 * Still longer term this could perhaps get merged or become a part of the global language context.  It is not
 * efficient to access this through thread local state... it should be a member off of some context object
 * we pass as a hidden first parameter to "enhanced" methods.
 */
public class BindingContext  {
   BindingEvent queuedEvents;

   public static void setBindingContext(BindingContext ctx) {
      PTypeUtil.setThreadLocal("bindingContext", ctx);
   }

   public int getQueueSize() {
      synchronized (this) {
         BindingEvent e = queuedEvents;
         int ct = 0;
         while (e != null) {
            ct++;
            e = e.next;
         }
         return ct;
      }
   }

   public static BindingContext getBindingContext() {
      return (BindingContext) PTypeUtil.getThreadLocal("bindingContext");
   }

   private IListener.SyncType defaultSyncType;
   public IListener.SyncType getDefaultSyncType() {
      return defaultSyncType;
   }
   public void setDefaultSyncType(IListener.SyncType t) {
      defaultSyncType = t;
   }

   public static IListener.SyncType getCurrentDefaultSyncType() {
      BindingContext ctx = getBindingContext();
      // TODO: This should be set to IMMEDIATE by default so we can avoid the extra queuing step
      // at least for frameworks that don't need the queued behavior.  One option: BindSettings on the
      // base class.  Any bindings src'd on that class will pick up the default listener.  Add a new
      // option or flags arg to the bind call and propagate that through to make it easy to add new
      // flags.
      if (ctx == null)
         return IListener.SyncType.IMMEDIATE;
      else
         return ctx.getDefaultSyncType();
   }

   public void queueEvent(int eventFlag, Object obj, Object prop, IListener listener, Object eventDetail) {
      BindingEvent newEvent = new BindingEvent(eventFlag, obj, prop, listener, eventDetail);
      BindingEvent oldEvent;
      BindingEvent prevEvent = null;

      if (Bind.trace)
         Bind.logMessage("Queuing event: ", obj, prop instanceof String ? (String) prop : null, eventDetail);

      synchronized (this) {
         for (oldEvent = queuedEvents; oldEvent != null; oldEvent = oldEvent.next) {
            // Higher priority numbers are at the top of the list - no need to check for dependencies in this case
            // If priorities are the same, retain the original event ordering, i.e. find the last element in the list and 
            // add the new guy after that one.  This is required now because we need invalidate messages to run before 
            // validate events though not sure we should be queuing invalidate events at all.
            if (newEvent.listener.getPriority() > oldEvent.listener.getPriority()) {
               if (prevEvent == null) {
                  newEvent.next = queuedEvents;
                  queuedEvents = newEvent;
               }
               else {
                  newEvent.next = oldEvent;
                  prevEvent.next = newEvent;
               }
               break;
            }
            prevEvent = oldEvent;
         }
         if (oldEvent == null) {
            if (prevEvent == null) {
               newEvent.next = queuedEvents;
               queuedEvents = newEvent;
            }
            else {
               prevEvent.next = newEvent;
            }
         }
      }
   }

   float UNSET_PRIORITY = Float.MIN_VALUE;

   /**
    * Called when we want to dispatch some of the pending events  We go through and pull out the events which
    * match the sync flag.  All events of the same priority are batched up in this method and sent to doDispatch
    * where dependencies are resolved.
    *
    * @param sync
    */
   public void dispatchEvents(Object sync) {
      BindingEvent oldEvent;
      BindingEvent toExecute = null, lastToExecute = null;
      float priority = UNSET_PRIORITY;
      BindingEvent prevEvent = null, nextEvent;
      List<BindingEvent> runList = null;
      boolean queuedLastEvent = false;

      synchronized (this) {
         /**
          * Go through the list and pull out any events which match this sync flag.
          */
         for (oldEvent = queuedEvents; oldEvent != null; oldEvent = nextEvent) {
            nextEvent = oldEvent.next;

            // If we are executing this event, pull it out of the list and put it on the toExecute list
            if (sync == null || oldEvent.listener.getSync() == null || oldEvent.listener.getSync().equals(sync)) {
               if (priority != oldEvent.listener.getPriority()) {
                  if (toExecute != null) {
                     if (runList == null)
                        runList = new ArrayList<BindingEvent>();
                     runList.add(toExecute);
                  }

                  // Set the new priority and start out with the new next guy
                  priority = oldEvent.listener.getPriority();
                  toExecute = oldEvent;
                  lastToExecute = oldEvent;
               }
               else {
                  // Should only get here the second time once we've run the above
                  assert lastToExecute != null;
                  lastToExecute.next = oldEvent;
                  lastToExecute = oldEvent;
               }

               if (prevEvent == null)
                   queuedEvents = nextEvent;
               else {
                  if (queuedEvents == oldEvent)
                     queuedEvents = nextEvent;

                  // Only if it is still in the queued events list, need to remove it
                  if (!queuedLastEvent)
                     prevEvent.next = nextEvent;
               }
               oldEvent.next = null;
               queuedLastEvent = true;
            }
            else
               queuedLastEvent = false;
            prevEvent = oldEvent;
         }
      }
      // If we have more than one priority, the list stores the lists that need to be run first
      if (runList != null) {
         int sz = runList.size();
         for (int i = 0; i < sz; i++) {
            BindingEvent be = runList.get(i);
            // Dispatch all of the events with the same priority.
            doDispatch(be);
         }
      }
      if (toExecute != null)
         doDispatch(toExecute);
   }

   /**
    * At this point we have the list of events with the same priority.  We need to figure out which to run first
    * by looking for any depedencies between the listeners.
    */
   private void doDispatch(BindingEvent list) {
      BindingEvent outer, inner;

      /* Make a pass through all pairs and compute the dependencies */
      /*
      for (outer = list; outer != null; outer = outer.next) {
         for (inner = outer.next; inner != null; inner = inner.next) {
            IListener innerListener = inner.listener;
            IListener outerListener = outer.listener;
            // Not sure we should be queueing the same listener more than once but if so, it should not
            // be a dependency.
            if (innerListener != outerListener) {
               ISet readsOI = outerListener.getReads();
               ISet readsIO = innerListener.getReads();
               if (readsOI != null && readsIO != null) {
                  boolean depOI = readsOI.containsAny(innerListener.getWrites());
                  boolean depIO = readsIO.containsAny(outerListener.getWrites());
                  if (depOI) {
                     if (!depIO) {
                        outer.addDependency(inner);
                     }
                  }
                  else if (depIO)
                     inner.addDependency(outer);
               }
            }
         }
      }
      */

      for (outer = list; outer != null; outer = outer.next) {
         dispatchDependencies(outer);
      }
   }

   final static List<BindingEvent> VISITED_MARKER = new ArrayList<BindingEvent>(0);

   private void dispatchDependencies(BindingEvent event) {
      List<BindingEvent> deps = event.dependencies;
      if ((event.flags & BindingEvent.DISPATCHED) == 0) {
         event.flags |= BindingEvent.DISPATCHED;

         if (deps != null) {
            for (int i = 0; i < deps.size(); i++)
               dispatchDependencies(deps.get(i));
         }

         event.dispatch();
      }
   }

}

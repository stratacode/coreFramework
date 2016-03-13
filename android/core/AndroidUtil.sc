
import android.os.Looper;
import android.os.MessageQueue;

import sc.bind.Bind;
import sc.type.IBeanMapper;

public class AndroidUtil {
   public static void sendDelayedEvent(final int eventType, final Object obj, final sc.type.IBeanMapper prop) {
      MessageQueue queue = Looper.myQueue();
      queue.addIdleHandler(new MessageQueue.IdleHandler() {
         public boolean queueIdle() {
            Bind.sendEvent(eventType, obj, prop);
            return false;  // remove this handler
         }
      });
   }

   public static void sendDelayedEvents(final int eventType, final Object obj, final sc.type.IBeanMapper[] props) {
      MessageQueue queue = Looper.myQueue();
      queue.addIdleHandler(new MessageQueue.IdleHandler() {
         public boolean queueIdle() {
            for (IBeanMapper prop:props) 
               Bind.sendEvent(eventType, obj, prop);
            return false;  // remove this handler
         }
      });
   }
}

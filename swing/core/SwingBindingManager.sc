import sc.bind.BindingManager;
import sc.bind.Bind;
import sc.bind.BindingContext;
import sc.bind.IListener;
import sc.type.IBeanMapper;

import java.awt.EventQueue;
import javax.swing.SwingUtilities;

public class SwingBindingManager extends BindingManager {
   public SwingBindingManager() {
      Bind.bindingManager = this;
   }

   public static BindingContext bindingContext = new BindingContext();

   private static volatile boolean handlerRegistered = false;

   public void sendEvent(IListener listener, int event, Object obj, IBeanMapper prop, Object eventDetail) {
      if (EventQueue.isDispatchThread()) {
         super.sendEvent(listener, event, obj, prop, eventDetail);
      }
      else {
         bindingContext.queueEvent(event, obj, prop, listener, eventDetail);

         if (!handlerRegistered) {
            handlerRegistered = true;
            SwingUtilities.invokeLater(new Runnable() {
               public void run() {
                  handlerRegistered = false;
                  bindingContext.dispatchEvents(null);
               }
            });
         }
      }
   }
}

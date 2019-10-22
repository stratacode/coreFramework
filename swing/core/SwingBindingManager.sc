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

   public static BindingContext bindingContext = new BindingContext(IListener.SyncType.IMMEDIATE);

   private static volatile boolean handlerRegistered = false;

   public void sendEvent(IListener listener, int event, Object obj, IBeanMapper prop, Object eventDetail) {
      BindingContext curCtx = BindingContext.getBindingContext();
      if (curCtx != bindingContext && curCtx != null) {
         if (curCtx.queueEnabledForEvent(event)) {
            curCtx.queueEvent(event, obj, prop, listener, eventDetail, null);
            return;
         }
         else {
            System.err.println("*** Unrecognized binding context in SwingBindingManager");
         }
      }

      if (EventQueue.isDispatchThread()) {
         super.sendEvent(listener, event, obj, prop, eventDetail);
      }
      else {
         bindingContext.queueEvent(event, obj, prop, listener, eventDetail, null);

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

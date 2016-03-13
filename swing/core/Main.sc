import sc.bind.Bind;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import sc.obj.MainSettings;
/** 
 * A simple main you can use for your swing components.
 * Children of this object are automatically instantiated at startup
 */
@Component
@CompilerSettings(objectTemplate="sc.swing.MainInitTemplate")
public object Main {
   /** Create and register a binding manager to ensure events are delivered only on the swing thread */
   object bindingManager extends SwingBindingManager {
   }
   object statementProcessor extends SwingStatementProcessor {
   }

   @MainSettings(produceScript=true,execName="swingmain")
   public static void main(String[] args) {
      //Schedule a job for the event dispatching thread:
      //creating and showing this application's GUI.
      SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               // Need to update the event handling thread with the current class loader so JPA and things can use it to find
               // resources
               ClassLoader cl = sc.dyn.DynUtil.getSysClassLoader();
               if (cl != null)
                  Thread.currentThread().setContextClassLoader(cl);

               Main main = Main; // Referencing the main object will create it and kick everything off
               // If any dynamic types are registered with the @MainInit when we start, grab them as well
               // Actually, we'll compile in any such references using dynamic new calls
               //Object[] dynInitObj = sc.dyn.DynUtil.resolveTypeGroupMembers("mainInit");
            }
        });
    }
}

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import sc.type.IBeanMapper;

@CompilerSettings(inheritProperties=false)
@Component
public class JMenuItem extends javax.swing.JMenuItem implements ComponentStyle {
   @Bindable
   public int clickCount;

   {
      addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            clickCount++;
         }
      });
   }
}

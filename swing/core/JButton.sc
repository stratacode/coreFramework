import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import sc.type.IBeanMapper;

@Component
public class JButton extends javax.swing.JButton implements ComponentStyle {
   @Bindable
   public int clickCount;

   override @Bindable size;
   override @Bindable(manual=true) preferredSize;
   override @Bindable location;

   {
      addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            // clickCount++; TODO fixme!
            clickCount = clickCount + 1;
         }
      });
   }

   @Bindable
   public void setText(String text) {
      super.setText(text);
      sc.bind.Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, this, SwingUtil.preferredSizeProp);
   }

   @Bindable
   public void setIcon(Icon icon) {
      super.setIcon(icon);
      sc.bind.Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, this, SwingUtil.preferredSizeProp);
   }
}

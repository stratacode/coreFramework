import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;

@Component
public class JScrollPane extends javax.swing.JScrollPane implements PanelStyle {
   {
      addComponentListener(new ComponentListener() {
         public void componentResized(ComponentEvent e) {
   	    invalidate();
	    validate();
         }
	 public void componentHidden(ComponentEvent e) {}
	 public void componentShown(ComponentEvent e) {}
	 public void componentMoved(ComponentEvent e) {}
      });
   }

   override @Bindable location;
   override @Bindable size;
   override @Bindable visible;
}

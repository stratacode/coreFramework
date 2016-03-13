import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;

@Component
public class RTextScrollPane extends org.fife.ui.rtextarea.RTextScrollPane {
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

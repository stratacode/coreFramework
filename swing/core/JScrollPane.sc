import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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

      getViewport().addChangeListener(new ChangeListener() {

          @Override
          public void stateChanged(ChangeEvent e) {
             boolean hvis = getHorizontalScrollBar().isVisible();
             boolean vvis = getVerticalScrollBar().isVisible();
             if (hvis != horizontalScrollBarVisible)
                horizontalScrollBarVisible = hvis;
             if (vvis != verticalScrollBarVisible)
                verticalScrollBarVisible = vvis;
          }
      });
   }

   override @Bindable location;
   override @Bindable size;
   override @Bindable visible;

   @Bindable public boolean horizontalScrollBarVisible;
   @Bindable public boolean verticalScrollBarVisible;
}

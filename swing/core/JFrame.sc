import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;

@Component
@CompilerSettings(objectTemplate="sc.swing.JFrameInit", newTemplate="sc.swing.JFrameNew", dynChildManager="sc.swing.SwingDynChildManager")
public class JFrame extends javax.swing.JFrame {
  {
     addComponentListener(new ComponentListener() {  
        public void componentResized(ComponentEvent evt) {
            java.awt.Component c = (java.awt.Component)evt.getSource();
            setSize(c.getSize());
        }
        public void componentMoved(ComponentEvent evt) {
            java.awt.Component c = (java.awt.Component)evt.getSource();
            setLocation(c.getLocation());
        }
        public void componentShown(ComponentEvent evt) {
            java.awt.Component c = (java.awt.Component)evt.getSource();
            setVisible(true);
        }
        public void componentHidden(ComponentEvent evt) {
            java.awt.Component c = (java.awt.Component)evt.getSource();
            setVisible(false);
        }
      });
  }
}

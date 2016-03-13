import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import sc.bind.Bind;

@Component
public class JColorChooser extends javax.swing.JColorChooser {
   static sc.type.IBeanMapper colorProperty = sc.type.TypeUtil.getPropertyMapping(JColorChooser.class, "color");

   override @Bindable color;

   {
      // Swing does not support binding events on its text property.
      getSelectionModel().addChangeListener(new ChangeListener() {
         public void stateChanged(ChangeEvent e) {
            SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JColorChooser.this, colorProperty);
         }
      });
   }
}

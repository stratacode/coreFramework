import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import sc.bind.Bind;

@Component
public class JPasswordField extends javax.swing.JPasswordField {
   static sc.type.IBeanMapper textProperty = sc.type.TypeUtil.getPropertyMapping(JPasswordField.class, "text");

   {
      // Swing does not support binding events on its text property.
      getDocument().addDocumentListener(new DocumentListener() {
         public void insertUpdate(DocumentEvent e) {
            SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JPasswordField.this, textProperty);
         }
         public void removeUpdate(DocumentEvent e) {
            SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JPasswordField.this, textProperty);
         }
         public void changedUpdate(DocumentEvent e) {}
      });
   }

}

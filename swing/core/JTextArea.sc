import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

@CompilerSettings(inheritProperties=false)
@Component
public class JTextArea extends javax.swing.JTextArea implements TextComponentStyle {
   public static sc.type.IBeanMapper textProperty = sc.type.TypeUtil.getPropertyMapping(JTextArea.class, "text");
   public static sc.type.IBeanMapper preferredSizeProperty = sc.type.TypeUtil.getPropertyMapping(JTextArea.class, "preferredSize");
   public static sc.type.IBeanMapper caretPositionProperty = sc.type.TypeUtil.getPropertyMapping(JTextArea.class, "caretPosition");

   private boolean suppressEvents = false;

   {
      // Swing does not support binding events on its text property.
      getDocument().addDocumentListener(new DocumentListener() {
         public void insertUpdate(DocumentEvent e) {
            SwingUtil.updateUserAction();
            if (!suppressEvents)
               SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JTextArea.this, textProperty);
         }
         public void removeUpdate(DocumentEvent e) {
            SwingUtil.updateUserAction();
            if (!suppressEvents)
               SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JTextArea.this, textProperty);
         }
         public void changedUpdate(DocumentEvent e) {}
      });
   }

   override @Bindable preferredSize;
   override @Bindable size;
   override @Bindable location;
   override @Bindable visible;

   /** 
    * Bi-directional bindings do not like intermediate events.  Swing sends
    * a remove and an insert event for the setText call.  This sets up a race
    * so that in some cases, the remove gets processed while the value is null
    * undoing the change.  So we have to stop sending events in this process
    * and send our own afterwards.
    */
   @Bindable(manual=true)
   public void setText(String t) {
      try {
         suppressEvents = true;
         super.setText(t);
      }
      finally {
         suppressEvents = false;
         SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, this, textProperty);
         SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, this, preferredSizeProperty);
         SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, this, caretPositionProperty);
      }
      //invalidate();
      //validate();
   }
}

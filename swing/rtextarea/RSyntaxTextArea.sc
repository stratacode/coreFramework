import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import sc.bind.Bind;

@Component
@CompilerSettings(useAltComponent=true)
class RSyntaxTextArea extends org.fife.ui.rsyntaxtextarea.RSyntaxTextArea implements TextComponentStyle {
   wrapStyleWord = true;
   private boolean suppressEvents = false;

   {
      // Swing does not support binding events on its text property.
      getDocument().addDocumentListener(new DocumentListener() {
         public void insertUpdate(DocumentEvent e) {
            if (!suppressEvents) {
               SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, RSyntaxTextArea.this, JTextArea.textProperty);
               textChangedCount++;
            }
         }
         public void removeUpdate(DocumentEvent e) {
            if (!suppressEvents) {
               SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, RSyntaxTextArea.this, JTextArea.textProperty);
               textChangedCount++;
            }
         }
         public void changedUpdate(DocumentEvent e) {}
      });
   }

   override @Bindable preferredSize;
   override @Bindable size;
   override @Bindable location;
   override @Bindable visible;

   @Bindable
   int textChangedCount = 0;

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
         Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, this, JTextArea.textProperty);
         Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, this, JTextArea.preferredSizeProperty);
         Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, this, JTextArea.caretPositionProperty); // Or maybe we could turn off that caret mode?
      }
      //invalidate();
      //validate();
   }

   /** If position is set outside of the document range, just put it at the end instead of throwing an exception.  Makes it easier to use this property with data binding. */
   public void setCaretPosition(int pos) {
      Document d = getDocument();
      int len = d == null ? -1 : d.getLength();
      if (len != -1 && pos >= len)
         pos = len - 1;
      super.setCaretPosition(pos);
   }
}

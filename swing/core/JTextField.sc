import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;

import sc.bind.Bind;

@Component
public class JTextField extends javax.swing.JTextField implements TextComponentStyle {
   static sc.type.IBeanMapper textProperty = sc.type.TypeUtil.getPropertyMapping(JTextField.class, "text");
   static sc.type.IBeanMapper userEnteredProperty = sc.type.TypeUtil.getPropertyMapping(JTextField.class, "userEnteredCount");

   private boolean suppressEvents = false;

   // Making these bindable by default so the swing demos don't need to get recompiled... with better layout managers
   // binding to these would be less common and so maybe not good by default?
   override @Bindable location;
   override @Bindable preferredSize;

   override @Bindable size;
   override @Bindable visible;

   @Bindable
   public boolean focus = false;

   @Bindable
   public String enteredText;  /** Bind to this to get text events only when the focus is changed or enter is pressed */
   text := enteredText;

   /** This event is fired whenever the user changes the text value - not when text is changed otherwise */
   @Bindable
   public int userEnteredCount = 0;

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
      }
   }

   {
      // Swing does not support binding events on its text property.
      getDocument().addDocumentListener(new DocumentListener() {
         public void insertUpdate(DocumentEvent e) {
            if (!suppressEvents)
               SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JTextField.this, textProperty);
         }
         public void removeUpdate(DocumentEvent e) {
            if (!suppressEvents)
               SwingUtil.sendDelayedEvent(sc.bind.IListener.VALUE_CHANGED, JTextField.this, textProperty);
         }
         public void changedUpdate(DocumentEvent e) {}
      });
   }

   {
      addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent evt) {
            fireUserChange();
         }
      });
   }


   public void fireUserChange() {
      enteredText = text;
      userEnteredCount++;
   }

   {
      addFocusListener(new FocusListener() {
                   public void focusGained(FocusEvent e) {
                      focus = true;
                   }

                   public void focusLost(FocusEvent e) {
                      focus = false;
                   }});

   }

}

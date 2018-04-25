import sc.bind.*;
import sc.type.IBeanMapper;
import sc.type.TypeUtil;

/** Wrapper for javax.swing.JLabel to make it a bindable StrataCode component. */
@Component
public class JLabel extends javax.swing.JLabel implements ComponentStyle {
   public static IBeanMapper textProp = TypeUtil.getPropertyMapping(JLabel.class, "text");

   /**
    *  Overrides the swing setText method to fire change events for both preferredSize and text.
    *  No need for automatic binding events so turn that off.
    */
   @Bindable(manual = true)
   public void setText(String text) {
      super.setText(text);
      Bind.sendEvent(IListener.VALUE_CHANGED, this, SwingUtil.preferredSizeProp);
      Bind.sendEvent(IListener.VALUE_CHANGED, this, textProp);
   }

   @Bindable
   public void setIcon(Icon icon) {
      super.setIcon(icon);
      Bind.sendEvent(IListener.VALUE_CHANGED, this, SwingUtil.preferredSizeProp);
   }

   /** Binding events are already sent above so mark it as Bindable but turn off code-gen */
   override @Bindable(manual=true) preferredSize;

   /** Will code-gen a setX method that calls super.setX and sends a change event */
   override @Bindable size;
   override @Bindable location;
   override @Bindable visible;
   override @Bindable foreground;
   override @Bindable background;
}

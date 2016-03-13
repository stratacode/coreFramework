import sc.bind.*;
import sc.type.IBeanMapper;
import sc.type.TypeUtil;

@Component
public class JLabel extends javax.swing.JLabel implements ComponentStyle {
   public static IBeanMapper textProp = TypeUtil.getPropertyMapping(JLabel.class, "text");

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

   override @Bindable(manual=true) preferredSize;
   override @Bindable size;
   override @Bindable location;
   override @Bindable visible;
   override @Bindable foreground;
   override @Bindable background;
}

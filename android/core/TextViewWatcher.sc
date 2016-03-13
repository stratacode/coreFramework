import android.text.TextWatcher;
import android.text.Editable;
import sc.bind.*;
import sc.type.*;

class TextViewWatcher implements TextWatcher {
   TextView view;
   IBeanMapper prop;
   TextViewWatcher(TextView view, IBeanMapper prop) {
      this.view = view;
      this.prop = prop;
   }
   void afterTextChanged(Editable s) {
      AndroidUtil.sendDelayedEvent(IListener.VALUE_CHANGED, view, prop);
   }
   void beforeTextChanged(CharSequence s, int start, int count, int after) {
   }
   void onTextChanged(CharSequence s, int start, int before, int count) {
   }
}

import java.util.Map;

import sc.bind.Bind;
import sc.bind.BindSettings;
import sc.bind.IListener;

@sc.obj.Sync(syncMode= sc.obj.SyncMode.Disabled)
public class HashMap<K,V> extends java.util.HashMap<K,V> implements sc.bind.IChangeable {
   @BindSettings(reverseMethod="putNoEvent", reverseSlot=1)
   public V get(Object key) {
      return super.get(key);
   }

   /** 
    * Because this method gets called in response to a reverse binding event firing, we cannot send an event here.
    * It triggers an event infinite loop when you have a bi-directional binding hooked up to "get"  
    */
   public V putNoEvent(K key, V value) {
      return super.put(key, value);
   }


   public V put(K key, V value) {
      V res = super.put(key, value);

      Bind.sendEvent(IListener.VALUE_CHANGED, this, null);
      return res;
   }

   public V remove(Object key) {
      V res = super.remove(key);
      Bind.sendEvent(IListener.VALUE_CHANGED, this, null);
      return res;
   }

   public void putAll(Map<? extends K, ? extends V> m) {
      super.putAll(m);

      Bind.sendEvent(IListener.VALUE_CHANGED, this, null);
   }

   public void clear() {
      super.clear();

      Bind.sendEvent(IListener.VALUE_CHANGED, this, null);
   }
}

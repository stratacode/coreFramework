public interface IListItemScope<T> extends IChildContainer {
   public T getListItemValue(); 
   public void setListItemValue(T v);
   public Object getListItemPrev(); 
   public void setListItemPrev(Object v);
}

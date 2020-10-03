import sc.bind.AbstractListener;
import sc.lang.html.IPageDispatcher;

public class URLRefreshListener extends AbstractListener {
   IPage pageObject;
   PageDispatcher.PageEntry pageEnt;
   boolean refreshScheduled = false;

   public URLRefreshListener(IPage pageObj, PageDispatcher.PageEntry pageEnt) {
      this.pageObject = pageObj;
      this.pageEnt = pageEnt;
   }

   public boolean valueValidated(Object obj, Object prop, Object eventDetail, boolean apply) {
      String propName = prop instanceof IBeanMapper ? ((IBeanMapper) prop).propertyName : (String) prop;

      Object oldValue = pageObject.getPageProperties().get(propName);
      Object newValue = DynUtil.getPropertyValue(pageObject, propName);
      if (DynUtil.equalObjects(oldValue, newValue))
         return false;

      if (!refreshScheduled) {
         refreshScheduled = true;
         DynUtil.invokeLater(new Runnable() {
            public void run() {
               updateURLFromProperties();
               refreshScheduled = false;
            }
         }, 0);
      }

      return true;
   }

   public void updateURLFromProperties() {
      String newPath = pageEnt.evalPatternWithInst(null, pageObject);
      Window window = Window.getWindow();
      if (window != null) {
         if (window.location.pathname.equals(newPath))
            return;
         window.location.updatePath(newPath);

         window.history.replaceState(pageObject.pageProperties, "newTitle", window.location.href);
      }
   }
}
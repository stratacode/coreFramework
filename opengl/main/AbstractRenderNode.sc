import java.util.ArrayList;

public abstract class AbstractRenderNode implements IRenderNode {

   static List<IRenderParent> EMPTY_LIST = new ArrayList<IRenderParent>(0);

   protected boolean changed;
   public boolean visible = true;

   visible =: markChanged();

   public boolean isChanged() {
      return changed;
   }
   public void markChanged() {
      if (changed || !visible)
         return;

      IRenderParent par = getParent();
      if (par != null)
         par.markChanged();

      List<IRenderParent> links = getLinks();
      if (links != null) {
         for (IRenderParent link:links) {
            link.markChanged();
         }
      }
   }

   private IRenderParent parent;
   public IRenderParent getParent() {
      return parent;
   }
   public void setParent(IRenderParent par) {
      this.parent = par;
   }

   private ArrayList<IRenderParent> links = null;
   public List<IRenderParent> getLinks() {
      if (links == null)
         return EMPTY_LIST;
      return links;
   }

   public void addLink(IRenderParent link) {
      if (links == null)
         links = new ArrayList<IRenderParent>();
      links.add(link);
   }
   public boolean removeLink(IRenderParent par) {
      if (links == null)
         return false;
      return links.remove(par);
   }

   public void init(GLAutoDrawable draw) {
   }

}

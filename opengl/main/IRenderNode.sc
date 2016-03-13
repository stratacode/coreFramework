public interface IRenderNode {
   public static float OVERLAY_SPACE = 2.0f;
   boolean isChanged();
   void markChanged();
   IRenderParent getParent();
   List<IRenderParent> getLinks();
   void setParent(IRenderParent parent);
   void addLink(IRenderParent link);
   boolean removeLink(IRenderParent par);
   void init(GLAutoDrawable draw);
   void display(GLAutoDrawable draw);
}

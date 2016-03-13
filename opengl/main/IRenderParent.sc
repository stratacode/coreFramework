
public interface IRenderParent {
   boolean isChanged();
   void markChanged();

   void addChild(IRenderNode node);
   boolean removeChild(IRenderNode node);
   List<IRenderNode> getChildren();

   GLAutoDrawable getCanvas();
}

import sc.util.ArrayList;

@Component
@CompilerSettings(objectTemplate="sc.opengl.GroupInit", newTemplate="sc.opengl.GroupNew", dynChildManager="sc.opengl.GLDynChildManager")
public class CGroup extends AbstractRenderNode implements IRenderParent {
   static sc.type.IBeanMapper childrenProperty = sc.type.TypeUtil.getPropertyMapping(CGroup.class, "children");

   private List<IRenderNode> children;

   public double x = 0.0, y = 0.0, z = 0.0, scaleX = 1.0, scaleY = 1.0, scaleZ = 1.0;

   x =: markChanged();
   y =: markChanged();
   z =: markChanged();
   scaleX =: markChanged();
   scaleY =: markChanged();
   scaleZ =: markChanged();

   public boolean applyGroupScale = true;

   public List<IRenderNode> getChildren() {
      return children;
   }

   public void addChild(IRenderNode node) {
      if (children == null) {
         children = new ArrayList<IRenderNode>();
         sc.bind.Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, this, childrenProperty);
      }
      node.setParent(this);
      children.add(node);
   }

   public boolean removeChild(IRenderNode node) {
      if (children == null)
         return false;
      return children.remove(node);
   }

   public void display(GLAutoDrawable glDrawable) {
      changed = false;

      if (children == null || !visible)
         return;
      final GL2 gl = glDrawable.getGL().getGL2();

      gl.glPushMatrix();
      applyGroupTransform(gl);
      // TODO: should propagate a mask up the tree and only push those values
      gl.glPushAttrib(GL2.GL_ALL_ATTRIB_BITS);
      for (IRenderNode node:children) {
         node.display(glDrawable);
      }
      gl.glPopAttrib();
      gl.glPopMatrix();
   }

   protected void applyGroupTransform(GL2 gl2) {
      if (x != 0.0 || y != 0.0 || z != 0.0)
         gl2.glTranslated(x, y, z);
      if (applyGroupScale && (scaleX != 1.0 || scaleY != 1.0 || scaleZ != 1.0))
         gl2.glScaled(scaleX, scaleY, scaleZ);
   }

   public void init(GLAutoDrawable glDrawable) {
      if (children == null)
         return;
      for (IRenderNode node:children) {
         node.init(glDrawable);
      }
   }

   public GLAutoDrawable getCanvas() {
      IRenderNode node = this;
      do {
         IRenderParent parent = node.getParent();
         if (parent instanceof GLAutoDrawable)
            return (GLAutoDrawable) parent;
         if (!(parent instanceof IRenderNode))
            break;
         node = (IRenderNode) parent;
      } while (true);
      return null;
   }
}

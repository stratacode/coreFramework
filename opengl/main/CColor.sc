public class CColor extends AbstractRenderNode {
   public double r =: markChanged(), g =: markChanged(), b =: markChanged();

   public void display(GLAutoDrawable draw) {
      changed = false;
      final GL2 gl = draw.getGL().getGL2();
      gl.glColor3d(r, g, b);
   }
}

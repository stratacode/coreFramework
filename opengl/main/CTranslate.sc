public class CTranslate extends AbstractRenderNode {
   public double x =: markChanged(), y =: markChanged(), z =: markChanged();

   public void display(GLAutoDrawable draw) {
      changed = false;
      final GL2 gl = draw.getGL().getGL2();
      gl.glTranslated(x, y, z);
   }
}

public class CRotate extends AbstractRenderNode {
   public float x =: markChanged(), y =: markChanged(), z =: markChanged();

   public void display(GLAutoDrawable draw) {
      changed = false;
      final GL2 gl = draw.getGL().getGL2();
      gl.glRotatef(x, 1.0f, 0.0f, 0.0f);
      gl.glRotatef(y, 0.0f, 1.0f, 0.0f);
      gl.glRotatef(z, 0.0f, 0.0f, 1.0f);
   }
}

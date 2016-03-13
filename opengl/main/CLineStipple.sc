public class CLineStipple extends AbstractRenderNode {
   public boolean enabled = true;

   public short stipple = (short) 0x2929;
   public int factor = 1;

   enabled =: markChanged();

   public void display(GLAutoDrawable draw) {
      changed = false;
      final GL2 gl = draw.getGL().getGL2();
      gl.glLineStipple(factor, stipple);
      gl.glEnable(GL2.GL_LINE_STIPPLE);
   }
}

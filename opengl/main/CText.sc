import com.jogamp.opengl.util.awt.TextRenderer;
import java.awt.Font;

public class CText extends AbstractRenderNode {
   TextRenderer renderer;

   public String fontName = "SanSerif";
   public int fontSize = 36;
   public boolean bold, italic;
   fontName =: fontChanged();
   fontSize =: fontChanged();
   bold =: fontChanged();
   italic =: fontChanged();

   public boolean use3D = true;

   public String text;
   public double x = 0.0, y = 0.0, z = 0.0, scale = 1.0;
   public double width;
   x =: markChanged();
   y =: markChanged();
   z =: markChanged();
   scale =: markChanged();
   text =: markChanged();

   double r, g, b, a;

   public void init(GLAutoDrawable draw) {
      fontChanged();
   }

   public void fontChanged() {
      renderer = new TextRenderer(new Font(fontName, (bold ? Font.BOLD : 0) | (italic ? Font.ITALIC : 0), fontSize));
      markChanged();
   }

   public void display(GLAutoDrawable draw) {
      changed = false;
      if (text == null || !visible)
         return;
      if (use3D) {
         boolean clipEnabled = width != 0;
         final GL2 gl = draw.getGL().getGL2();
         if (clipEnabled) {
            double[] clipEqn = {-1.0, 0.0, 0.0, (x + width)};
            gl.glClipPlane(GL2ES1.GL_CLIP_PLANE0, clipEqn, 0);
            gl.glEnable(GL2ES1.GL_CLIP_PLANE0);
         }
         renderer.begin3DRendering();
         renderer.setColor((float)r, (float)g, (float)b, (float)a);
         renderer.draw3D(text, (float) x, (float) y, (float) z, (float) scale);
         renderer.end3DRendering();
         if (clipEnabled) {
            gl.glDisable(GL2ES1.GL_CLIP_PLANE0);
         }
      }
      else {
         System.err.println("*** No more 2D text mode");
      /*
         // these getWidth and getHeight methods no longer exist
         renderer.beginRendering(draw.getWidth(), draw.getHeight());
         renderer.setColor((float)r, (float)g, (float)b, (float)a);
         renderer.draw3D(text, (float) x, (float) y, (float) z, (float) scale);
         renderer.endRendering();
      */
      }
   }
}

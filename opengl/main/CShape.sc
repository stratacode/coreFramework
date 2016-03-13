public class CShape extends AbstractRenderNode {
   public Shapes shape = Shapes.GL_QUADS; 
   shape =: markChanged();

   public double UNSET_COLOR = -1;

   public double r = UNSET_COLOR;
   public double g = UNSET_COLOR;
   public double b = UNSET_COLOR;
   r =: markChanged();
   g =: markChanged(); 
   b =: markChanged();

   private static float[] defaultNormal = {0.0f, 0.0f, -1.0f};

   public float[][] verts;
   public float[][] normals;
   verts =: markChanged();

   public void display(GLAutoDrawable draw) {
      changed = false;
      if (verts == null || verts.length == 0 || !visible)
         return;
      final GL2 gl = draw.getGL().getGL2();

      if (r != UNSET_COLOR && g != UNSET_COLOR && b != UNSET_COLOR)
         gl.glColor3d(r, g, b);

      // either normal per vertex, triangle, quad etc.
      int incr = normals == null ? 1 : verts.length / normals.length;

      gl.glBegin(shape.getValue());
         for (int i = 0; i < verts.length; i++) {
            float[] vert = verts[i];
            switch (vert.length) {
               case 2:
                  gl.glVertex2f(vert[0], vert[1]);
                  break;
               case 3:
                  gl.glVertex3f(vert[0], vert[1], vert[2]);
                  break;
               default:
                  System.err.println("*** invalid vertex array size: " + vert.length);
                  return;
            }
            if (normals != null) {
               int ix = i/incr;
               float[] normal = normals[ix];
               switch (normal.length) {
                  case 3:
                     gl.glNormal3f(normal[0], normal[1], normal[2]);
                     break;
                  default:
                     System.err.println("*** invalid normal array size: " + vert.length);
                     return;
               }
            }
            else
               gl.glNormal3f(defaultNormal[0], defaultNormal[1], defaultNormal[2]);
         }
      gl.glEnd();
   }

   public enum Shapes {
      GL_POINTS {
         int getValue() {
            return GL.GL_POINTS;
         }
      },

      GL_LINES {
         int getValue() {
            return GL.GL_LINES;
         }
      },

      GL_LINE_STRIP {
         int getValue() {
            return GL.GL_LINE_STRIP;
         }
      },

      GL_LINE_LOOP {
         int getValue() {
            return GL.GL_LINE_LOOP;
         }
      },

      GL_TRIANGLES {
         int getValue() {
            return GL.GL_TRIANGLES;
         }
      },

      GL_TRIANGLE_STRIP {
         int getValue() {
            return GL.GL_TRIANGLE_STRIP;
         }
      },

      GL_TRIANGLE_FAN {
         int getValue() {
            return GL.GL_TRIANGLE_FAN;
         }
      },

      GL_QUADS {
         int getValue() {
            return GL2.GL_QUADS;
         }
      },

      GL_QUAD_STRIP {
         int getValue() {
            return GL2.GL_QUAD_STRIP;
         }
      },

      GL_POLYGON {
         int getValue() {
            return GL2.GL_POLYGON;
         }
      };

      abstract int getValue();
   }

}


public class CRect extends CGroup {
   public double width = 1.0, height = 1.0;
   /*
   scaleX :=: width;
   scaleY :=: height;
   scaleZ := (width + height) / 2;
   */

   width =: validate();
   height =: validate();

   void validate() {
     bkg.validate();
     border.validate();
   }

   enum BorderStyle {
      BEVEL, LINE;
   }

   public object bkg extends CShape {
      r = 0.95;
      g = 0.95;
      b = 0.95;

      void validate() {
         verts = new float[][] {{0.0f, 0.0f, 0.0f}, {0.0f, (float) height, 0.0f}, {(float) width, (float) height, 0.0f}, {(float) width, 0.0f, 0.0f}};
      }
   }
   public object border extends CShape {
      BorderStyle borderStyle = BorderStyle.BEVEL;
      borderStyle =: validate();
      float bevelWidth := (float) (22 * 0.06);
      float bevelDepth = (float) (-OVERLAY_SPACE);
      float normDepth := bevelWidth;

      public border() {
         validate();
      }

      void validate() {
         if (borderStyle == BorderStyle.BEVEL) {
            shape = Shapes.GL_QUADS;
            verts = new float[][] {{0.0f, 0.0f, 0.0f}, {0.0f, (float) height, 0.0f}, {-bevelWidth, (float) (height+bevelWidth), bevelDepth}, {-bevelWidth, -bevelWidth, bevelDepth},
                                   {0.0f, 0.0f, 0.0f}, {-bevelWidth, -bevelWidth, bevelDepth}, {(float) (width+bevelWidth), -bevelWidth, bevelDepth}, {(float) width, 0.0f, 0.0f},
                                   {(float) width, 0.0f, 0.0f}, {(float) (width+bevelWidth), -bevelWidth, bevelDepth}, {(float)(width+bevelWidth), (float)(height+bevelWidth), bevelDepth}, {(float) width, (float) height, 0.0f},
                                   {(float)(width+bevelWidth), (float)(height+bevelWidth), bevelDepth}, {(float) width, (float) height, 0.0f}, {0.0f, (float)height, 0.0f}, {-bevelWidth, (float)(height+bevelWidth), bevelDepth}};
            normals = new float[][] {C3DUtil.normalize(C3DUtil.crossProduct(new float[] {-bevelWidth, -bevelWidth, normDepth}, new float[] {0.0f, 1.0f, 0.0f})),
                                     C3DUtil.normalize(C3DUtil.crossProduct(new float[] {1.0f, 0.0f, 0.0f}, new float[] {-bevelWidth, -bevelWidth, normDepth})),
                                     C3DUtil.normalize(C3DUtil.crossProduct(new float[] {0.0f, 1.0f, 0.0f}, new float[] {bevelWidth, -bevelWidth, normDepth})),
                                     C3DUtil.normalize(C3DUtil.crossProduct(new float[] {-bevelWidth, bevelWidth, normDepth}, new float[] {1.0f, 0.0f, 0.0f}))};

/*
            for (int i = 0; i < normals.length; i++) {
               System.out.println("*** normal[" + i + "] = " + normals[i][0] + "," + normals[i][1] + "," + normals[i][2]);
            }
*/
         }
         else {
            shape = Shapes.GL_LINE_STRIP;
            verts = new float[][] {{0.0f, 0.0f, 0.0f}, {0.0f, 1.0f, 0.0f}, {1.0f, 1.0f, 0.0f}, {1.0f, 0.0f, 0.0f}, {0.0f, 0.0f, 0.0f}};
            normals = null;
         }
      }
   }
}

public class C3DUtil {

   public static float veclen(float[] vec) {
      return (float) Math.sqrt(vec[0] * vec[0] + vec[1] * vec[1] + vec[2] * vec[2]);
   }
   public static float[] normalize(float[] vec) {
      float len = veclen(vec);
      float[] res = new float[] {vec[0]/len, vec[1]/len, vec[2]/len};;
      return res;
   }

   public static float dotProduct(float[] vec1, float[] vec2) {
      return vec1[0] * vec2[0] + vec1[1] * vec2[1] + vec1[2] * vec2[2];
   }

   public static float[] crossProduct(float[] vec1, float[] vec2) {
      float[] res = new float[] {vec1[1]*vec2[2] - vec1[2]*vec2[1], 
                                  vec1[2]*vec2[0] - vec1[0]*vec2[2],
                                  vec1[0]*vec2[1] - vec1[1]*vec2[0]};
      return res;
   }

   /** Multiplies src1 by src2 putting the result in dst.  Note that dst can be the same
     * array as one of the sources.
     */
   public static void multiplyMatrices(float[] src1, float[] src2, float[] dst) {
      float[] res = new float[16];
      for (int i = 0; i < 16; i += 4) {
         for (int j = 0; j < 4; j++) {
           res[i + j] = 0.0f;
           for (int k = 0; k < 4; k++)
             res[i + j] += src1[i + k] * src2[k*4 + j];
         }
       }
       for (int i = 0; i < 16; i++) 
           dst[i] = res[i];
   }

   static private float[] identityMat = 
                    new float[] {1.0f, 0.0f, 0.0f, 0.0f, 
                                 0.0f, 1.0f, 0.0f, 0.0f,
                                 0.0f, 0.0f, 1.0f, 0.0f,
                                 0.0f, 0.0f, 0.0f, 1.0f};

   public static void matIdentity(float[] mat) {
       for (int i = 0; i < 16; i++) 
           mat[i] = identityMat[i];
   }

   public static float[] matTranslate(double x, double y, double z) {
      float[] mat = new float[16];
      matIdentity(mat);
      mat[3*4+0] = (float)x;
      mat[3*4+1] = (float)y;
      mat[3*4+2] = (float)z;
      return mat;
   }

   public static float[] matScale(double x, double y, double z) {
      float[] mat = new float[16];
      matIdentity(mat);
      mat[4*0+0] = (float)x;
      mat[4*1+1] = (float)y;
      mat[4*2+2] = (float)z;
      return mat;
   }

   public static float[] getScaleMatrix(int x1, int y1, int x2, int y2, int width, int height) {
      float[] mat = new float[16];
      float ox = width/2.0f;
      float oy = height/2.0f;
      float r = (ox + oy) / 2.0f;
      if (r == 0f)
         r = 1f;

      float xdiff = x2 - x1;
      float ydiff = y2 - y1;

      float absxdiff = Math.abs(xdiff);
      float absydiff = Math.abs(ydiff);
      float diff;

      if (absxdiff > absydiff) {
         diff = xdiff;
      }
      else {
         diff = ydiff;
      }

      float scale = 1f + diff / r;

      matIdentity(mat);
      mat[0] = scale;
      mat[4*1+1] = scale;
      mat[4*2+2] = scale;

      return mat;
   }

   public static float[] getTranslationMatrix(int x1, int y1, int x2, int y2, int width, int height, boolean doZ) {
      float[] mat = new float[16];
      matIdentity(mat);

      float ox = width/2.0f;
      float oy = height/2.0f;
      float r = (ox + oy) / 2.0f;

      if (doZ) {
         int delta = Math.abs(x2 - x1) > Math.abs(y1 - y2) ? x2 - x1 : y1 - y2;
         mat[3*4+2] = delta / r;
      }
      else {
         mat[3*4+0] = (x2 - x1) / r;
         mat[3*4+1] = (y1 - y2) / r;
      }

      return mat;
   }

   public static float[] getRotationMatrix(int x1, int y1, int x2, int y2, int width, int height) {
      float[] mat = new float[16];

      float ox = width/2.0f;
      float oy = height/2.0f;
      float r = (ox + oy) / 2.0f;

      // Need to flip these upside down!
      y1 = height - y1;
      y2 = height - y2;

      // X and Y coordinates of the origin's "sphere vector" (selection point projected ont
      // the sphere
      float xf1 = x1 - r;
      float yf1 = y1 - r;

      // Compute the length of the vector from the center of the circle to the first point
      float s = (float) Math.sqrt(xf1*xf1 + yf1 * yf1);

      boolean backSide;
      if (s <= r) {
         backSide = true;
      }
      else {
         float sorig = s;
         float sout = s - r;
         //backSide = (sout / 2.0*r) & 1 == 0;

         // How many diameters are we outside
         int numD = (int) (sout / (2.0f * r));
         int numR = (int) (sout / r);
         // backside
         if ((numD & 1) == 0) {
            backSide = false;
         }
         else {
            backSide = true;
         }

         // For odd iterations, we are moving towards the center and s is shrinking
         if ((numR & 1) == 0) {
            s = r - (sout - (numR * r));
            xf1 = xf1 * s/sorig;
            yf1 = yf1 * s/sorig;
         }
         // For even iterations we are moving away from the center and s gets bigger 
         else {
            s = sout - numR * r;
            xf1 = -xf1 * s/sorig;
            yf1 = -yf1 * s/sorig;
         }

         if (s > r)
            System.out.println("*** s screwed up");
      }

      // Compute z-coordinate of vc
      float zf1 = (float) Math.sqrt(r*r - s*s);

      if (!backSide)
         zf1 = -zf1;

      float[] vec1 = new float[] { xf1, yf1, zf1};

      float xf2 = x2 - r;
      float yf2 = y2 - r;

      // Compute the length of the vector from the center of the circle to the first point
      s = (float) Math.sqrt(xf2*xf2 + yf2*yf2);

      if (s <= r) {
         backSide = true;
      }
      else {
         float sorig = s;
         float sout = s - r;
         //backSide = (sout / 2.0*r) & 1 == 0;

         // How many diameters are we outside
         int numD = (int) (sout / (2.0f * r));
         int numR = (int) (sout / r);
         // backside
         if ((numD & 1) == 0) {
            backSide = false;
         }
         else {
            backSide = true;
         }

         // For odd iterations, we are moving towards the center and s is shrinking
         if ((numR & 1) == 0) {
            s = r - (sout - (numR * r));
            xf2 = xf2 * s/sorig;
            yf2 = yf2 * s/sorig;
         }
         // For even iterations we are moving away from the center and s gets bigger 
         else {
            s = sout - numR * r;
            xf2 = -xf2 * s/sorig;
            yf2 = -yf2 * s/sorig;
         }

         if (s > r)
            System.out.println("*** s screwed up");
      }


      // Compute z-coordinate of vc
      float zf2 = (float) Math.sqrt(r*r - s*s);

      // We use the backside of the sphere once the cursor is outside of the physical sphere.
      if (!backSide)
         zf2 = -zf2;

      float[] vec2 = new float[] { xf2, yf2, zf2};

      float len1 = veclen(vec1);
      float len2 = veclen(vec2);
      if (len1 < 0.001 || len2 < 0.001)
         System.out.println("*** bad veclen!");

      float[] cross = crossProduct(vec2,vec1);
      if (veclen(cross) < 0.0001) {
         matIdentity(mat);
         return mat;
      }
      float[] axis = normalize(cross);

      float cosangle = dotProduct(vec1,vec2) / (veclen(vec1) * veclen(vec2));
      if (cosangle > 1.0f)
         cosangle = 1.0f;
      float sinangle = (float) Math.sqrt(1.0f - cosangle*cosangle);
      if (sinangle > 1.0f)
         sinangle = 1.0f;

      mat[0] = cosangle + axis[0]*axis[0] * (1.0f - cosangle);
      mat[1] = axis[0] * axis[1] * (1.0f - cosangle) - axis[2]*sinangle;
      mat[2] = axis[0] * axis[2] * (1.0f - cosangle) + axis[1]*sinangle;
      mat[3] = 0.0f;

      mat[4] = axis[1]*axis[0]*(1.0f - cosangle) + axis[2] * sinangle;
      mat[5] = cosangle + axis[1]*axis[1]*(1.0f - cosangle);
      mat[6] = axis[1]*axis[2] * (1.0f - cosangle) - axis[0]*sinangle;
      mat[7] = 0.0f;

      mat[8] = axis[2]*axis[0] * (1.0f - cosangle) - axis[1]*sinangle;
      mat[9] = axis[2]*axis[1] * (1.0f - cosangle) + axis[0]*sinangle;
      mat[10]= cosangle + axis[2]*axis[2] * (1.0f - cosangle);
      mat[11] = 0.0f;

      mat[12] = 0.0f;
      mat[13] = 0.0f;
      mat[14] = 0.0f;
      mat[15] = 1.0f;

      //System.out.println("*** Rotating: " + Math.acos(cosangle) / (2.0f*3.1415f) * 360.0f + " degress about: " + axis[0] + ", " + axis[1] + ", " + axis[2] + " for pixel move from: " + x1 + ", " + y1 + "->" + x2 + ", " + y2);

      return mat;
   }
}

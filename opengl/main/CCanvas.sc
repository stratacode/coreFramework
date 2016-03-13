import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLCapabilities;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;

import java.util.ArrayList;

@Component
@CompilerSettings(objectTemplate="sc.opengl.GroupInit", newTemplate="sc.opengl.GroupNew", dynChildManager="sc.opengl.GLDynChildManager")
public class CCanvas extends GLCanvas implements IRenderParent {
   public double viewMin = -1.25, viewMax = 1.25;
   public double zMin = 1.0;
   public double zMax = 1000.0;

   public double viewX = 0.0, viewY = 0.0, viewZ = -3.5, viewScale = 1.0;

   public boolean perspective = true;
   public boolean lighting = true;
   public boolean antialiasing = true;
   public double fov = 40;

   public float[] lightPos = {-50f, 50f, -80f, 1.0f};

   override @Bindable visible;

   perspective =: viewChanged();
   fov =: viewChanged();
   zMin =: viewChanged();
   zMax =: viewChanged();
   viewMin =: viewChanged();
   viewMax =: viewChanged();
   viewZ =: viewChanged();
   viewX =: viewChanged();
   viewY =: viewChanged();
   viewScale =: viewChanged();

   protected boolean changed = false, viewStateChanged = false;

   private float[] viewMatrix; 
   private float aspectRatio;

   private final static int UNSET_POSITION = -9999999;
   private int lastX = UNSET_POSITION;
   private int lastY = UNSET_POSITION;

   // Make size property bindable.  It is defined as non-bindable in the AWT Component class.
   override @Bindable size;

   private List<IRenderNode> children;

   public List<IRenderNode> getChildren() {
      return children;
   }

   static GLProfile profile = com.jogamp.opengl.GLProfile.getDefault();
   static GLCapabilities capabilities = new GLCapabilities(profile);
   static {
      capabilities.setSampleBuffers(true);
      capabilities.setNumSamples(4);
   }

   public CCanvas() {
      super(capabilities);
      // When we call display() it will turn around and call display(glDrawable) 
      // which kicks off the rendering.
      addGLEventListener(new CanvasListener());
      CMouseListener listener = new CMouseListener();
      addMouseListener(listener);
      addMouseMotionListener(listener);
      viewMatrix = new float[16];
      resetView();
   }

   public void resetView() {
      C3DUtil.matIdentity(viewMatrix);
   }

   public void addChild(IRenderNode node) {
      if (children == null)
         children = new ArrayList<IRenderNode>();
      children.add(node);
      node.setParent(this);
   }

   public boolean removeChild(IRenderNode node) {
      if (children == null)
         return false;
      node.setParent(null);
      return children.remove(node);
   }

   public void init() {
      markChanged();
   }

   static GLU glu = new GLU();

   public void markChanged() {
      if (changed)
         return;
      changed = true;
      refresh();
   }

   public void viewChanged() {
      viewStateChanged = true;
      markChanged();
   }

   public boolean isChanged() {
      return changed;
   }

   public void refresh() {
      SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               changed = false;
               display();
            }
        });
   }

   public float backgroundRed =: markChanged(), backgroundGreen =: markChanged(), backgroundBlue =: markChanged();

   class CMouseListener implements MouseListener, MouseMotionListener {
      public void mouseClicked(MouseEvent e) {
         lastX = e.getX();
         lastY = e.getY();
      }

      public void mouseEntered(MouseEvent e) {
      }

      public void mouseExited(MouseEvent e) {
      }

      public void mousePressed(MouseEvent e) {
         lastX = e.getX();
         lastY = e.getY();
         if (e.getClickCount() == 2) {
            resetView();
            markChanged();
         }
      }

      public void mouseReleased(MouseEvent e) {
         lastX = UNSET_POSITION;
         lastY = UNSET_POSITION;
      }

      public void mouseMoved(MouseEvent e) {
      }

      public void mouseDragged(MouseEvent e) {
         float[] matrix;

         if (e.getButton() == e.BUTTON1) {
            if (e.isShiftDown()) {
               matrix = C3DUtil.getScaleMatrix(lastX, lastY, e.getX(), e.getY(), (int)size.width, (int)size.height);
            }
            else {
               matrix = C3DUtil.getRotationMatrix(lastX, lastY, e.getX(), e.getY(), (int)size.width, (int)size.height);
            }
         }
         else if (e.getButton() == e.BUTTON2 || e.getButton() == e.BUTTON3) {
            matrix = C3DUtil.getTranslationMatrix(lastX, lastY, e.getX(), e.getY(), (int)size.width, (int)size.height, e.isShiftDown());
         }
         else {
            System.err.println("*** Unrecognized draw event");
            return;
         }

         C3DUtil.multiplyMatrices(viewMatrix, matrix, viewMatrix);
         markChanged();
         lastX = e.getX();
         lastY = e.getY();
      }

      public void mouseWheelMoved(MouseEvent e) {
      }
   }

   class CanvasListener implements GLEventListener {
      public void display(GLAutoDrawable glDrawable) {
         final GL2 gl = glDrawable.getGL().getGL2();

         if (viewStateChanged) {
            viewStateChanged = false;

            if (antialiasing) {
               gl.glEnable(GL.GL_MULTISAMPLE);
               gl.glEnable(GL2ES1.GL_POINT_SMOOTH);
               gl.glEnable(GL.GL_LINE_SMOOTH);
               gl.glEnable(GL2GL3.GL_POLYGON_SMOOTH);
            }
            else {
               gl.glDisable(GL.GL_MULTISAMPLE);
               gl.glDisable(GL2ES1.GL_POINT_SMOOTH);
               gl.glDisable(GL.GL_LINE_SMOOTH);
               gl.glDisable(GL2GL3.GL_POLYGON_SMOOTH);
            }

            gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            gl.glLoadIdentity();

            gl.glPolygonOffset(1.0f, 1.0f);

            double xMin = aspectRatio > 1.0f ? viewMin : viewMin * aspectRatio;
            double xMax = aspectRatio > 1.0f ? viewMax : viewMax * aspectRatio;
            double yMin = aspectRatio > 1.0f ? viewMin * aspectRatio : viewMin;
            double yMax = aspectRatio > 1.0f ? viewMax * aspectRatio : viewMax;

            if (perspective) {
               //
               //gl.glFrustum(xMin, xMax, yMin, yMax, zMax, zMin);

               // Apply the perspective transformation
               glu.gluPerspective((float)fov, aspectRatio, (float)zMin, (float)zMax);
            }
            else {
               // Min and max for z switched on purpose.  For some reason perspective did not work right with it the other way
               gl.glOrtho(xMin, xMax, yMin, yMax, zMin, zMax);
            }

            gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
         }

         gl.glLoadIdentity();
         gl.glMultMatrixf(C3DUtil.matScale(viewScale,viewScale,viewScale), 0);
         // Set the view point by translating the origin to the viewpoint
         gl.glMultMatrixf(C3DUtil.matTranslate(viewX, viewY, viewZ), 0);
         gl.glClearColor(backgroundRed,  backgroundGreen, backgroundBlue, 0.0f);

         gl.glClear(GL.GL_COLOR_BUFFER_BIT);
         gl.glClear(GL.GL_DEPTH_BUFFER_BIT);

         gl.glPushMatrix();
         gl.glMultMatrixf(viewMatrix, 0);

         if (lighting) {
           gl.glEnable(GLLightingFunc.GL_LIGHT0);
           gl.glEnable(GLLightingFunc.GL_LIGHTING);

           // Prepare light parameters.
           float[] lightColorAmbient = {0.1f, 0.1f, 0.1f, 1f};
           float[] lightColorSpecular = {0.0f, 0.0f, 0.0f, 1f};
           float[] lightColorDiffuse = {0.9f, 0.9f, 0.9f, 1f};

           // Set light parameters.
           gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_POSITION, lightPos, 0);
           gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_AMBIENT, lightColorAmbient, 0);
           gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_SPECULAR, lightColorSpecular, 0);
           gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_DIFFUSE, lightColorDiffuse, 0);
           gl.glLightf(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_CONSTANT_ATTENUATION, 1.f );

           //gl.glShadeModel(GLLightingFunc.GL_SMOOTH);
           gl.glShadeModel(GLLightingFunc.GL_FLAT);

           gl.glEnable(GLLightingFunc.GL_COLOR_MATERIAL);

           // Set material properties.
           float[] rgba = {0.1f, 0.1f, 1f};
           gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_AMBIENT, rgba, 0);
           gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_SPECULAR, rgba, 0);
           gl.glMaterialf(GL.GL_FRONT, GLLightingFunc.GL_SHININESS, 0.0f);

           gl.glMaterialfv(GL.GL_BACK, GLLightingFunc.GL_AMBIENT, rgba, 0);
           gl.glMaterialfv(GL.GL_BACK, GLLightingFunc.GL_SPECULAR, rgba, 0);
           gl.glMaterialf(GL.GL_BACK, GLLightingFunc.GL_SHININESS, 0.0f);


           // I'm used to having a separate normal matrix you can set which only includes the rotation components.
           // this is expensive...
           //gl.glEnable(GL2ES1.GL_RESCALE_NORMAL);
           gl.glEnable(GLLightingFunc.GL_NORMALIZE);
         }

         if (children == null)
            return;
         // TODO: should propagate a mask up the tree and only push those values
         gl.glPushAttrib(GL2.GL_ALL_ATTRIB_BITS);


         for (IRenderNode node:children) {
            node.display(glDrawable);
         }
         gl.glPopAttrib();
         gl.glPopMatrix();
      }

      public void dispose(GLAutoDrawable glDrawable) {
      }

      public void init(GLAutoDrawable glDrawable) {
         GL2 gl = glDrawable.getGL().getGL2();
         // TODO: put some of these into properties so they are configurable.   Probably need to
         // shadow the state since we can't get the GL outside of this call.
         //gl.glShadeModel(GLLightingFunc.GL_SMOOTH);
         gl.glClearDepth(1.0f);
         gl.glEnable(GL.GL_DEPTH_TEST);
         gl.glDepthFunc(GL.GL_LEQUAL);
         gl.glHint(GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);

         if (children != null) {
            for (IRenderNode node:children) {
               node.init(glDrawable);
            }
         }
      }

      public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height) {
         GL2 gl = glDrawable.getGL().getGL2();
         if (height <= 0) {
            height = 1;
         }

         aspectRatio = (float) width / (float) height;

         viewChanged();
      }
   }

   public GLAutoDrawable getCanvas() {
      return this;
   }
}

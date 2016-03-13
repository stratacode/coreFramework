//@MainInit
object TermTest extends AppFrame {
   size = SwingUtil.dimension(800,600);
   object term extends VIMPanel {
      fileNames = {"swing/core/SwingUtil.java"};
      size := SwingUtil.dimension(TermTest.this.size.width, TermTest.this.size.height-30);

      void init() {
         startCommand();
      }
   }

   void update(java.awt.Graphics g) {
      paint(g);
   }
}

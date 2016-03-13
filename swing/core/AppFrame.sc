public class AppFrame extends JFrame implements PanelStyle {
   @Bindable
   public int gap = 10;
   @Bindable
   public int windowWidth := (int)size.width;
   @Bindable
   public int windowHeight := (int)size.height;

   visible = true;
   defaultCloseOperation = JFrame.EXIT_ON_CLOSE;
   size = SwingUtil.dimension(300, 300);
   layout = null; // turns off swing's automatic layout

   public void stop() {
     visible = false;
     dispose();
   }
}

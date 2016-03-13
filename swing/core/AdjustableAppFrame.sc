public class AdjustableAppFrame extends AppFrame {
   xpad := xpadSlider.value;
   ypad := ypadSlider.value;
   gap := gapSlider.value;
   baseline := baselineSlider.value;

   public int sliderWidth := (int)((size.width-xpad)/4);
   public int sliderHeight = 30;

   // Must be set by the extender
   public int sliderY;

   size := SwingUtil.dimension(480,500);

   public object xpadSlider extends JSlider {
      minimum = 0;
      maximum = 20;
      orientation = HORIZONTAL;
      value = 10;

      location := SwingUtil.point(xpad, sliderY);
      size := SwingUtil.dimension(sliderWidth, sliderHeight);
   }

   public object ypadSlider extends JSlider {
      minimum = 0;
      maximum = 20;
      orientation = HORIZONTAL;
      value = 10;

      location := SwingUtil.point(xpad + sliderWidth, sliderY);
      size := SwingUtil.dimension(sliderWidth, sliderHeight);
   }

   public object baselineSlider extends JSlider {
      minimum = 0;
      maximum = 20;
      orientation = HORIZONTAL;
      value = 5;

      location := SwingUtil.point(xpad + 2*sliderWidth, sliderY);
      size := SwingUtil.dimension(sliderWidth, sliderHeight);
   }

   public object gapSlider extends JSlider {
      minimum = 0;
      maximum = 20;
      orientation = HORIZONTAL;
      value = 10;

      location := SwingUtil.point(xpad + 3*sliderWidth, sliderY);
      size := SwingUtil.dimension(sliderWidth, sliderHeight);
   }

   public object colorChoice extends JToggleButton {
      size := preferredSize;
      location := SwingUtil.point((windowWidth - preferredSize.width - 2*xpad)/2, xpadSlider.location.y + xpadSlider.size.height + ypad);
      text := selected ? "Foreground color" : "Background color";
   }

   public object colorChooser extends JColorChooser {
      size := SwingUtil.dimension(preferredSize.width, preferredSize.height-100);;
      location := SwingUtil.point(xpad, colorChoice.location.y + colorChoice.size.height);

      color :=: colorChoice.selected ? AdjustableAppFrame.this.foreground : AdjustableAppFrame.this.background;
      previewPanel = new JPanel();
   }
}

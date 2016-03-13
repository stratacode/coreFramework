public class CLabel extends CGroup {
   public double width, height;

   public int numLines = 1;
   public String text;

   public object rect extends CRect {
      width := CLabel.this.width;
      height := CLabel.this.height;
   }

   public double borderTop, borderBottom, borderLeft, borderRight;

   public object labelText extends CText {
      r = 0.15;
      g = 0.15;
      b = 0.15;
      a = 1.0;
      x := borderLeft;
      y := borderTop;
      width := CLabel.this.width - borderLeft - borderRight;
      text := CLabel.this.text;
      z = OVERLAY_SPACE;
      scale := 1.0/fontSize*((height-(borderTop + borderBottom))/numLines);
   }

}

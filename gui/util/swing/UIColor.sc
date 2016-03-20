// Not modifying core's Color - instead had to copy it.  We interpret "super" in this
// case as the modified type and so have no way to insert the actual super() call for the
// Color constructor.  This is the first case which begs for a modified() keyword which works 
// differently than super().   Or maybe for constructors we just use this() to also search the modified
// type - i.e. like how we treat super today?
@sc.obj.Sync(onDemand=true)
class UIColor extends java.awt.Color {
   // 0-255 values
   public int r, g, b;

   public UIColor(int r, int g, int b) {
      super(r, g, b);
      this.r = r;
      this.g = g;
      this.b = b;
   }

   public UIColor(java.awt.Color acol) {
      super(acol.getRed(), acol.getGreen(), acol.getBlue());
      this.r = acol.getRed();
      this.g = acol.getGreen();
      this.b = acol.getBlue();
   }
}

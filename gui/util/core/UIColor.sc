// These objects lazily synchronized - when referenced by another instance
@sc.obj.Sync(syncMode=sc.obj.SyncMode.Disabled)
// A platform independent implementation of a color.
class UIColor {
   // 0-255 values
   public int r, g, b;

   public UIColor(int r, int g, int b) {
      this.r = r;
      this.g = g;
      this.b = b;
   }
}

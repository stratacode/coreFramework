// Platform independent specification for an icon
@sc.obj.Sync(syncMode=sc.obj.SyncMode.ServerToClient)
class UIIcon {
   String dir; // Directory for the icon
   String path; // The path - the relative url part.
   String desc;  // A description for the alt text for the icon.

   UIIcon(String dir, String p, String d) {
      this.dir = dir;
      this.path = p;
      this.desc = d;
   }

   public boolean equals(Object other) {
      return other instanceof UIIcon && ((UIIcon) other).path.equals(path);
   }
   
   public int hashCode() {
      return path.hashCode();
   }
}

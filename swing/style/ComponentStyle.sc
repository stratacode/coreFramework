ComponentStyle {
   public static final Color defaultForeground = UIManager.getColor("Label.foreground");
   public static final Color defaultBackground = UIManager.getColor("Label.background");
   foreground := ComponentStyle.defaultForeground; 
   background := ComponentStyle.defaultBackground;
   override font;
   override opaque;
   override border;
}

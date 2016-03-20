UIIcon {
   Icon icon;
   UIIcon(String dir, String p, String d) {
       super(dir, p, d);
       String fullPath = dir + path;
       java.net.URL url = UIIcon.class.getResource(fullPath);
       if (url == null)
          System.err.println("Unable to open UIIcon as resource with path: " + fullPath);
       icon = new ImageIcon(url, desc);
    }
}

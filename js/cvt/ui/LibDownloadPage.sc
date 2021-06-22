@URL(pattern="/javaToJS/libFiles/{urlPath}",resource=true,dynContent=false)
scope<global> class LibDownloadPage extends DownloadPage {
   startUrl = "/javaToJS/libFiles";
   {
      mimeTypes.put("js", "text/javascript");
   }

   String getFilePath(String urlPath) {
      int ix = urlPath.indexOf("/");
      if (ix == -1) {
         return null;
      }

      String layerName = urlPath.substring(0, ix);
      String jsFileName = urlPath.substring(ix+1);

      return CvtManager.getJSLibFile(layerName, jsFileName);
   }
}

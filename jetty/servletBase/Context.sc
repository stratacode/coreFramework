import org.eclipse.jetty.server.Request;

Context {
   void platformEnableFileUpload(UploadServerConfig config) {
      request.setAttribute(Request.MULTIPART_CONFIG_ELEMENT, config.getServletConfig());
   }
}

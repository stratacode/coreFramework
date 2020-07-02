import javax.servlet.MultipartConfigElement;

class UploadServerConfig {
   String tempDir; 
   long maxFileSize; 
   long maxRequestSize; 
   int fileSizeThreshold;

   MultipartConfigElement config;

   MultipartConfigElement getServletConfig() {
      if (config == null) {
         config = new MultipartConfigElement(tempDir, maxFileSize, maxRequestSize, fileSizeThreshold);
      }
      return config;
   }
}

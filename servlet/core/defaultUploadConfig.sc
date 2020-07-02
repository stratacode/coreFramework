object defaultUploadConfig extends UploadServerConfig {
   tempDir = "/tmp/strataCodeFileUpload";
   maxFileSize = 20 * 1024 * 1024; // 20 MB - roughly the max image size
   maxRequestSize = 10 * 1024 * 1024; // 100MB - allow 10 max sized images to be uploaded at once
   fileSizeThreshold = 64 * 1024; // after this size, files are written to disk
}

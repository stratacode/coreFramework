import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import sc.util.FileUtil;
import sc.util.URLUtil;
import sc.util.StringUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.util.HashMap;
import java.io.PrintWriter;
import java.io.File;
import java.io.OutputStream;
import java.io.InputStream;

import java.io.ByteArrayOutputStream;

import sc.lang.html.BasePage;
import sc.lang.html.OutputCtx;

import sc.servlet.Context;
import java.util.Map;

abstract class UploadPage extends BasePage {

   String uploadPath;

   Path uploadDir = null;

   public UploadResult processUpload(Map<String,String> uploadedFiles, Map<String,String> formFields) {
      return new UploadResult("success", null);
   }

   public StringBuilder output(OutputCtx octx) {
      Context ctx = Context.getCurrentContext();

      if (uploadDir == null) {
         File f = new File(uploadPath);
         if (!f.isDirectory())
            f.mkdirs();
         uploadDir = Paths.get(uploadPath);
      }

      HttpServletRequest req = ctx.request;
      if (!req.getMethod().equalsIgnoreCase("POST")) {
         ctx.sendError(400, "File upload expected to use POST method not: " + req.getMethod());
         ctx.requestComplete = true;
         return null;
      }
      if (!req.getContentType().startsWith("multipart/form-data")) {
         ctx.sendError(400, "File upload expected to multipart/form-data content: " + req.getContentType());
         ctx.requestComplete = true;
         return null;
      }

      ctx.startFileUpload(null);

      Map<String,String> uploadedFiles = new HashMap<String,String>();
      Map<String,String> formFields = new HashMap<String,String>();

      // TODO: make sure user is logged in and add site or some top-level directory to separate the images being uploaded

      try {
         for (Part part: req.getParts()) {
            String partName = part.getName();
            long partSize = part.getSize();
            String contentType = part.getContentType();
            String fileName = part.getSubmittedFileName();
            String useFileName = fileName == null ? null : URLUtil.cleanFileName(fileName);

            if (!StringUtil.isEmpty(useFileName)) {
                Path outputFile = uploadDir.resolve(fileName);
                try (InputStream inputStream = part.getInputStream();
                     OutputStream outputStream = Files.newOutputStream(outputFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                   if (FileUtil.copyStream(inputStream, outputStream)) {
                      uploadedFiles.put(partName, fileName);
                      ctx.log("Uploaded file: " + outputFile + " for: " + partName);
                   }
                   else
                      ctx.error("Failed to copy stream to: " + outputFile);
                }
            }
            else {
               try (InputStream inputStream = part.getInputStream(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                  if (FileUtil.copyStream(inputStream, bos)) {
                     String value = new String(bos.toByteArray());
                     formFields.put(partName, value);
                     ctx.log("upload form field: " + partName + " : " + value);
                  }
               }
            }
         }

         UploadResult result = processUpload(uploadedFiles, formFields);

         ctx.response.setContentType("text/plain");
         return result.getResultJSON();
      }
      catch (ServletException exc) {
         ctx.sendError(500, "Upload failed: " + exc);
      }
      catch (IOException exc) {
         ctx.sendError(500, "Upload failed: " + exc);
      }
      return null;
   }
}

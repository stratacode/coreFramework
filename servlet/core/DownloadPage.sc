import javax.servlet.http.HttpServletRequest;

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
import java.io.FileInputStream;

import java.io.ByteArrayOutputStream;

import sc.lang.html.BasePage;
import sc.lang.html.OutputCtx;

import sc.servlet.Context;
import java.util.Map;

abstract class DownloadPage extends BasePage {

   String downloadPath;
   String startUrl = "";

   Map<String,String> mimeTypes = new HashMap<String,String>();

   Path downloadDir = null;

   public StringBuilder output(OutputCtx octx) {
      Context ctx = Context.getCurrentContext();

      if (downloadDir == null) {
         File f = new File(downloadPath);
         if (!f.isDirectory())
            ctx.error("No download directory: " + downloadPath);
         downloadDir = Paths.get(downloadPath);
      }

      HttpServletRequest req = ctx.request;
      HttpServletResponse res = ctx.response;
      if (!req.getMethod().equalsIgnoreCase("GET")) {
         ctx.sendError(400, "File download expected GET method not: " + req.getMethod());
         return null;
      }

      String path = req.getRequestURI();
      if (startUrl != null) {
         if (!path.startsWith(startUrl)) {
            ctx.sendError(404, "File not found");
            return null;
         }
         path = path.substring(startUrl.length()+1);
      }

      path = URLUtil.cleanFileName(path);
      path = FileUtil.unnormalize(path);

      String fileType = FileUtil.getExtension(path);

      String mimeType = mimeTypes.get(fileType);
      if (mimeType == null) {
         ctx.sendError(404, "File extension: " + fileType + " not supported");
      }

      String filePath = FileUtil.concat(downloadPath, path);
      File file = new File(filePath);

      long prevLastModTime = req.getDateHeader("If-Modified-Since");
      long fileModTime = file.lastModified();

      if (prevLastModTime != -1 && prevLastModTime <= fileModTime) {
         ctx.requestComplete = true;
         res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
         return null;
      }

      res.setContentType(mimeType);

      res.addHeader("Content-Length", Long.toString(file.length()));
      res.addDateHeader("Last-Modified", fileModTime);

      try (FileInputStream fis = new FileInputStream(file); OutputStream out = res.getOutputStream()) {
         byte[] buffer = new byte[32*1024];
         int numRead;

         while (((numRead = fis.read(buffer)) > 0)) {
            out.write(buffer, 0, numRead);
         }
      }
      catch (IOException exc) {
         if (!ctx.response.isCommitted())
            ctx.sendError(500, "Error reading file on server");
         else
            ctx.requestComplete = true;
         return null;
      }
      ctx.requestComplete = true;
      return null;
   }
}

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import sc.util.FileUtil;
import sc.util.URLUtil;
import sc.util.StringUtil;

import java.util.HashMap;
import java.io.PrintWriter;
import java.io.File;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedReader;

import java.io.ByteArrayOutputStream;

import sc.lang.html.BasePage;
import sc.lang.html.OutputCtx;

import sc.servlet.Context;
import java.util.Map;

import sc.util.JSON;

abstract class JSONPage extends BasePage {
   Object inputType;
   Object resultType;

   protected JSONPage(Object inputClass, Object resultClass) {
      this.inputType = inputClass;
      this.resultType = resultClass;
   }

   protected abstract Object processInput(Object input);

   public StringBuilder output(OutputCtx octx) {
      Context ctx = Context.getCurrentContext();

      HttpServletRequest req = ctx.request;

      if (!req.getMethod().equalsIgnoreCase("POST")) {
         ctx.sendError(400, "JSON page expected POST method not: " + req.getMethod());
         return null;
      }
      String contentType = req.getContentType();
      if (!contentType.startsWith("application/json") && !contentType.startsWith("text/plain")) {
         ctx.sendError(400, "JSON page expected content type to be application/json or text/plain not: " + contentType);
         return null;
      }

      if (inputType == null || resultType == null) {
         ctx.sendError(400, "JSON page is misconfigured");
         return null;
      }

      try {
         StringBuffer jsonSB = new StringBuffer();
         String line = null;
         try {
           BufferedReader reader = req.getReader();
           while ((line = reader.readLine()) != null)
              jsonSB.append(line);
         }
         catch (IOException e) {  
            ctx.log("Error reading POST data for track event: " + e);
            ctx.sendError(500, "Reading event data failed");
         }

         Object inputObj = JSON.toObject(inputType, jsonSB.toString(), null);
         if (inputObj == null) {
            throw new IllegalArgumentException("No json object found in request");
         }

         Object result = processInput(inputObj);

         StringBuilder resultJSON = result == null ? null : JSON.toJSON(result, resultType, null);
         if (resultJSON != null) {
            HttpServletResponse response = ctx.response;
            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.print(resultJSON);
            writer.flush();
            ctx.requestComplete = true;
            return null;
         }
         else {
            ctx.error("Invalid JSON - no response");
            ctx.sendError(400, "JSON request failed to product reply");
         }
      }
      catch (IllegalArgumentException exc) {
         ctx.error("Invalid JSON parameter in request: " + exc);
         ctx.sendError(400, "JSON request failed ");
      }
      catch (IOException exc) {
         ctx.sendError(500, "JSON request failed with IO error: " + exc);
      }
      return null;
   }
}

/*
 * Copyright (c) 2013. Jeffrey Vroom
 */

package sc.util;

/** Simplified version of the stratacode core utilities for running in JS */
public class FileUtil {
   public static final String LINE_SEPARATOR = "\n";
   public static final String PATH_SEPARATOR = ":";
   public static final String FILE_SEPARATOR = "/";
   public static final char PATH_SEPARATOR_CHAR = PATH_SEPARATOR.charAt(0);
   public static boolean nonStandardFileSeparator = false;

   public static String getExtension(String srcFileName) {
      int ix = srcFileName.lastIndexOf(".");
      if (ix == -1)
         return null;
      return srcFileName.substring(ix+1);
   }

   public static String replaceExtension(String fileName, String s) {
      int ix = fileName.lastIndexOf(".");
      if (ix == -1)
         return fileName + "." + s;
      else
         return fileName.substring(0, ix+1) + s;
   }

   public static String addExtension(String fileName, String s) {
      return fileName + "." + s;
   }

   public static String removeExtension(String fileName) {
      int ix = fileName.lastIndexOf(".");
      if (ix == -1)
         return fileName;
      else
         return fileName.substring(0, ix);
   }

   public static String concat(String... params) {
      String result = null;
      for (String param:params) {
         if (param != null && param.length() > 0) {
            if (result == null)
               result = param;
            else if (result.endsWith(FileUtil.FILE_SEPARATOR))
               result = result + param;
            else
               result = result + FileUtil.FILE_SEPARATOR + param;
         }
      }
      return result;
   }

   /** Returns the last component of the path name - the directory name or file name part of the path */
   public static String getFileName(String pathName) {
      while (pathName.endsWith(FILE_SEPARATOR))
         pathName = pathName.substring(0, pathName.length() - 1);

      int ix = pathName.lastIndexOf(FILE_SEPARATOR);
      if (ix != -1)
         pathName = pathName.substring(ix+1);

      return pathName;
   }

   /** Given a/b/c.x returns a/b */
   public static String getParentPath(String relFileName) {
      if (relFileName == null)
         return null;
      int ix = relFileName.lastIndexOf(FileUtil.FILE_SEPARATOR);
      if (ix == -1)
         return null;
      return relFileName.substring(0,ix);
   }

   public static String concatNormalized(String... params) {
      String result = null;
      for (String param:params) {
         if (param != null && param.length() > 0) {
            if (result == null)
               result = param;
            else if (result.endsWith("/"))
               result = result + param;
            else
               result = result + '/' + param;
         }
      }
      return result;
   }
}

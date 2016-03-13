package sc.servlet;

import sc.servlet.PathServlet;
import sc.servlet.PathServletFilter;

/** 
 * Do we store the source and classes in the standard J2EE web app?  Used by GWT, Wicket and frameworks that require
 * the standard source/class file location.
 */
servlet.webAppBuild extends webApp {
   compiledOnly = true;

   // Prefixes to be prepended onto the dirs where we store java source and classes
   buildSrcSubDir="web/WEB-INF/src";
   buildClassesSubDir="web/WEB-INF/classes";
}

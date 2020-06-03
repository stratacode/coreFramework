package sc.servlet;

import sc.servlet.PathServlet;
import sc.servlet.PathServletFilter;

/** Defines basic web-app support.  Used by all servlet based frameworks for generating web.xml */
servlet.webApp extends servlet.lib, util, html.schtml {
   compiledOnly = true;
   hidden = true;

   codeType = sc.layer.CodeType.Framework;

   public void start() {
      sc.layer.LayeredSystem system = getLayeredSystem();

      sc.lang.DefaultAnnotationProcessor servletProc = new sc.lang.DefaultAnnotationProcessor();
      servletProc.typeGroupName = "servlets";
      servletProc.validOnField = false;
      servletProc.requiredType = "javax.servlet.Servlet";
      registerAnnotationProcessor("sc.servlet.PathServlet", servletProc);

      sc.lang.DefaultAnnotationProcessor servletFiltersProc = new sc.lang.DefaultAnnotationProcessor();
      servletFiltersProc.typeGroupName = "servletFilters";
      servletFiltersProc.validOnField = false;
      servletFiltersProc.requiredType = "javax.servlet.Filter";
      registerAnnotationProcessor("sc.servlet.PathServletFilter", servletFiltersProc);

      // When either the list of servlets or servlet filters changes, we need to regenerate web.xml
      addTypeGroupDependency("web/WEB-INF/web.scxml", "web.WEB-INF.web", "servlets");
      addTypeGroupDependency("web/WEB-INF/web.scxml", "web.WEB-INF.web", "servletFilters");

      addSrcPath("web", "web", "web", null);
   }
}

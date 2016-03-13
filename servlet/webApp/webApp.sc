package sc.servlet;

import sc.servlet.PathServlet;
import sc.servlet.PathServletFilter;

/** Defines basic web-app support.  Used by all servlet based frameworks for generating web.xml */
servlet.webApp extends util, html.schtml {
   compiledOnly = true;

   codeType = sc.layer.CodeType.Framework;
   codeFunction = sc.layer.CodeFunction.Program;

   public void init() {
      // Exclude the javascript runtime.  All layers which extend this layer explicitly will also be excluded, unless they explicitly include a layer which uses JS
      excludeRuntimes("js", "gwt", "android");

      // The servlet stuff requires the default runtime, Server process
      //addRuntime(null);
      addProcess(sc.layer.ProcessDefinition.create("Server"));
   }

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

      if (activated) {
         // When either the list of servlets or servlet filters changes, we need to regenerate
         // web.xml
         system.addTypeGroupDependency("web/WEB-INF/web.scxml", "web.WEB-INF.web", "servlets");
         system.addTypeGroupDependency("web/WEB-INF/web.scxml", "web.WEB-INF.web", "servletFilters");
      }

      addSrcPath("web", "web", "web");
   }
}

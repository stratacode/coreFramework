package sc.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import sc.lang.html.IPage;

import sc.servlet.Context;
import sc.servlet.UploadPage;
import sc.servlet.DownloadPage;

public servlet.core extends webApp, meta, html.core {
   compiledOnly = true;
   hidden = true;

   codeType = sc.layer.CodeType.Framework;

   public void init() {
      // Split this layer and it's sublayers out into a new process using the default 'java' runtime
      addProcess(sc.layer.ProcessDefinition.create("Server", "java", true));

      if (activated) {
         // Turns on URL access to the layered system - only for the active layer
         layeredSystem.serverEnabled = true;
      }
   }

   public void start() {

      // Like request but where instances are stored in the browser's session
      sc.lang.sc.BasicScopeProcessor sessionScope = new sc.lang.sc.BasicScopeProcessor("session");
      sessionScope.validOnClass = true;
      sessionScope.validOnField = false;
      sessionScope.validOnObject = true;
      sessionScope.includeScopeAnnotation = true;
      sessionScope.needsField = false;
      sessionScope.customResolver = 
          "      javax.servlet.http.HttpSession _session = sc.servlet.Context.getCurrentSession();\n" +
          "      if (_session == null) return null;\n" +
          "      <%= variableTypeName %> _<%= lowerClassName %> = (<%= variableTypeName %>) _session.getAttribute(\"<%= typeClassName %>\");\n";
      sessionScope.customSetter = 
          "      _session.setAttribute(\"<%= typeClassName %>\", _<%= lowerClassName %>);\n";
      registerScopeProcessor("session", sessionScope);

      // Like session but stored per-window, per-session 
      sc.lang.sc.BasicScopeProcessor windowScope = new sc.lang.sc.BasicScopeProcessor("window");
      windowScope.validOnClass = true;
      windowScope.validOnField = false;
      windowScope.validOnObject = true;
      windowScope.includeScopeAnnotation = true;
      windowScope.needsField = false;
      windowScope.customResolver = 
          "      <%= variableTypeName %> _<%= lowerClassName %> = (<%= variableTypeName %>) sc.servlet.Context.getWindowScope(true).getValue(\"<%= typeClassName %>\");\n";
      windowScope.customSetter = 
          "      sc.servlet.Context.getWindowScope(true).setValue(\"<%= typeClassName %>\", _<%= lowerClassName %>);\n";
      registerScopeProcessor("window", windowScope);

      // Like session but stores objects per-url, per-session
      sc.lang.sc.BasicScopeProcessor appSessionScope = new sc.lang.sc.BasicScopeProcessor("appSession");
      appSessionScope.validOnClass = true;
      appSessionScope.validOnField = false;
      appSessionScope.validOnObject = true;
      appSessionScope.includeScopeAnnotation = true;
      appSessionScope.needsField = false;
      appSessionScope.customResolver = 
          "      sc.obj.ScopeContext _ctx = sc.servlet.AppSessionScopeDefinition.getAppSessionScope();\n" +
          "      if (_ctx == null) return null;\n" +
          "      <%= variableTypeName %> _<%= lowerClassName %> = (<%= variableTypeName %>) _ctx.getValue(\"<%= typeClassName %>\");\n";
      appSessionScope.customSetter = 
          "      _ctx.setValue(\"<%= typeClassName %>\", _<%= lowerClassName %>);\n";
      registerScopeProcessor("appSession", appSessionScope);

      sc.lang.DefaultAnnotationProcessor urlProc = new sc.lang.DefaultAnnotationProcessor();
      // Adds a static code snippet to register the page when you annotate a class with @URL.  If we happen to register an inner class the addPage still goes on the parent type
      urlProc.staticMixinTemplate = "sc.servlet.URLMixinTemplate";
      urlProc.validOnField = false;
      urlProc.validOnClass = true;
      urlProc.validOnObject = true;
      urlProc.initOnStartup = true;
      urlProc.typeGroupName = "URLTypes";
      urlProc.inherited = true; // Include any sub-type which has URL in the type group
      urlProc.skipAbstract = true; // Don't include any abstract classes or templates with abstract="true"
      registerAnnotationProcessor("sc.html.URL", urlProc);

      // Since the new sc.html.URL processor will include dependencies on sc.servlet in this package, if html.core gets compiled, we need to be compiled along
      // with it if we are in the same stack.
      addModifiedLayer("html.core");
   }
}

package org.eclipse.jetty.server;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.util.component.LifeCycle;
//import org.mortbay.jetty.security.UserRealm;
import org.eclipse.jetty.security.LoginService;

// Uncomment v8compat for jetty 8 support - though we could make this transparent easily enough jetty 8 is not supported 
// anymore
jetty.meta extends lib /*, v8compat */ {
   annotationLayer = true;
   compiledOnly = true;

   codeType = sc.layer.CodeType.Framework;
   codeFunction = sc.layer.CodeFunction.Program;
}

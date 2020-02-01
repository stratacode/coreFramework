// TODO: possibly merge this entire layer into jetty.servlet
jetty.webApp extends jetty.servlet, hikari.lib {
   compiledOnly = true;
   hidden = true;

   codeType = CodeType.Framework;

}

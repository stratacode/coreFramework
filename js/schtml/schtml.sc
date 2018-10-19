/**
 * Extend this layer for including the js runtime and building js versions of tag objects for your schtml pages.
 * Use it to run your template pages on the client.  Use servlet.schtml (or jetty.schtml) to run them on the server as well - i.e.
 * "isomorphically" generating both client and server versions at the same time that work together.  Use it by itself for a client
 * only app.
 */
js.schtml extends js.appPerPage.main, html.schtml {
   codeType = sc.layer.CodeType.Framework;
   compiledOnly = true;
}

// TODO: migrate js.template into this layer and remove it
js.schtml extends js.appPerPage.main, html.schtml {
   codeType = sc.layer.CodeType.Framework;
   codeFunction = sc.layer.CodeFunction.Program;
   compiledOnly = true;
}

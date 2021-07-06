// The layer used for the system used by the online java to JS converter
//
// No layer package so that temp files added to this layer end up in the empty package
// unless they provide their own package statement
// Using js.schtml instead of js.core so that we do pick up schtml template functionality as an option
// and to workaround a weird bug where the tag package prefix gets added in html.core, even though the 
// tag class for HtmlPage is defined in html.schtml. If this layer doesn't pick up html.schtml, it will
// still resolve to the sc.html.tag.HtmlPage class but there won't be source for it and so we hit some
// bug in getModifyType that tries to cast a class to a BodyTypeDeclaration
public js.cvt.javaToJS extends js.schtml {
   inheritPackage = false;
}

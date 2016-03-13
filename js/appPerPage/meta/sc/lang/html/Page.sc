/** Marking this with a module pattern - so subclasses will generate their own .js file with all dependent files */ 
@MainInit(subTypesOnly=true)
@sc.js.JSSettings(jsModulePattern="js/<%= typeName %>.js") Page {
}

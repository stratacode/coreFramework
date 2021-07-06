// The layer used for the system used by the online java to JS converter when not using
// modules
//
// No layer package so that temp files added to this layer end up in the empty package
// unless they provide their own package statement
public js.cvt.javaToJSNoModules extends js.options.disableModules, js.schtml {
   inheritPackage = false;
}

package sc.util;

import java.util.List;
import sc.util.ArrayList;

import sc.util.NumberConverter;
import sc.util.IntConverter;
import sc.util.ComponentList;

import sc.util.TextUtil;

import sc.dyn.DynUtil;

// these are here mostly for the generated code for properties and objects
import sc.type.IBeanMapper;
import sc.obj.TypeSettings;
import sc.bind.Bind;
import sc.bind.IBinding;
import sc.bind.BindingDirection;

@sc.js.JSSettings(jsModuleFile="js/scutil.js", prefixAlias="sc_")
util extends sys.std {
   // Note this needs to be an application layer so it is sorted after the js.sys layers... it needs to be able to find
   // the src versions of the java.* classes.
   codeType = sc.layer.CodeType.Application;
   hidden = true;

   compiledOnly = true;
   finalLayer = true;
}

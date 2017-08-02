package sc.util;

import java.util.List;
import sc.util.ArrayList;

import sc.util.NumberConverter;
import sc.util.ComponentList;

import sc.util.TextUtil;

import sc.bind.Bind;

import sc.dyn.DynUtil; // for the dispose method primarily

util extends sys.std {
   // Note this needs to be an application layer so it is sorted after the js.sys layers... it needs to be able to find
   // the src versions of the java.* classes.
   codeType = sc.layer.CodeType.Application;
   codeFunction = sc.layer.CodeFunction.Program;

   compiledOnly = true;
   finalLayer = true;
}

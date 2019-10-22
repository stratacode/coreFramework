// TODO: just copied the core log4j files and changed info to warn - we should 
// add an scproperties format which provides an include, merge/replace capability
// so this really just changes the root logger to warn only
log4j.warnLevel extends log4j.core {
   codeType = sc.layer.CodeType.Framework;
   hidden = true;
}

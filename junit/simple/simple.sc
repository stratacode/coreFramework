package sc.junit;

import sc.junit.*;
import static sc.junit.Assert.*;

junit.simple {
   codeType = CodeType.Framework;
   compiledOnly = true;
   exportPackage = false;

   public void start() {
      sc.lang.DefaultAnnotationProcessor proc = new sc.lang.DefaultAnnotationProcessor();
      proc.typeGroupName = "junitTestClasses";
      registerAnnotationProcessor("sc.junit.Test", proc);
   }
}

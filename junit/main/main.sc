
junit.main extends lib {
   exportPackage = false;
   public void start() {
      // This one needs to happen even for inactive layers for us to resolve the assertTrue and other methods in the TestCase base class.
      // That gets added by the TestAnnotationProcessor any time we put @Test on any methods in the class.
      if (!disabled)
         registerAnnotationProcessor("org.junit.Test", new sc.lang.TestAnnotationProcessor("Test", "junit"));
      if (activated) {
         layeredSystem.buildInfo.registerTestProcessor("junit", new sc.junit.JUnitTestProcessor());
      }
   }

   compiledOnly = true;
}

import sc.layer.ITestProcessor;
import sc.dyn.RDynUtil;
import sc.dyn.DynUtil;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.Failure;

public class JUnitTestProcessor extends RunListener implements ITestProcessor {
   boolean typesInited = false;
   public void initTypes() {
      if (typesInited)
         return;

      Object initTypesType = DynUtil.findType("sc.junit.InitTypes");
      if (initTypesType != null) {
         Object initTypesInst = DynUtil.createInstance(initTypesType, "");
         if (initTypesInst != null) {
            Object meth = DynUtil.resolveMethod(initTypesType, "initTypes", null, null);
            if (meth != null)
               DynUtil.invokeMethod(initTypesInst, meth);
         }
         typesInited = true;
      }
   }

   public boolean executeTest(Object cl) {
      initTypes();
      // For components, need to use the DynUtil wrapper to create the test class. 
      if (cl instanceof Class && !DynUtil.isComponentType(cl)) {
         JUnitCore core = new JUnitCore();
         core.addListener(this);
         Result res = core.run((Class) cl);
         if (!res.wasSuccessful())
            System.err.println(res.getFailures());
         return res.wasSuccessful();
      }
      else {
         boolean result = true;
         Object testInst = DynUtil.newInnerComponent(cl, null, null);
         Object[] methods = RDynUtil.getAllMethods(cl, "public", true);

         if (runAllMethods(cl, methods, org.junit.Before.class, testInst) &&
             runAllMethods(cl, methods, org.junit.Test.class, testInst)) { 
            System.out.println("Test: " + cl + " passed");
         }
         else
            result = false;
         return result;
      }
   }

   private boolean runAllMethods(Object cl, Object[] methods, Class annotClass, Object testInst) {
      boolean result = true;
      for (int i = 0; i < methods.length; i++) {
         try {
            // TODO: should catch exceptions and check the Test.expected value to see if it mentions that
            // probably other junit stuff we should do
            if (RDynUtil.getAnnotation(methods[i], annotClass) != null) {
               DynUtil.invokeMethod(testInst, methods[i]);
            }
         }
         catch (AssertionError e) {
            System.err.println("*** Test failed: " + cl + "." + methods[i] + ": " + e);
            result = false;
         }
      }
      return result;
   }

   public void testFailure(Failure failure) throws Exception {
       System.err.println("*** Test failed: " + failure.getMessage() + " trace: " + failure.getTrace());
   }
                   
}

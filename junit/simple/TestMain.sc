/** 
 */
@CompilerSettings(mixinTemplate="sc.junit.JUnitRunTemplate")
public object TestMain extends InitTypesBase {

   @sc.obj.MainSettings(produceScript=true,execName="runUnitTests")
   public static void main(String[] args) {
       System.out.println("Running Junit tests");
       TestMain.initTypes();
       System.out.println("Finished running Junit tests");
   }
}

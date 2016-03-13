import java.util.HashMap;
import java.util.Map;

public enum InverseOp {
   MULTIPLY("*", "/", false, "/", true),
   ADD("+", "-", false, "-", true),
   SUB("-", "+", true, "-", false),
   DIVIDE("/", "*", false, "/", false);

   static Map<String,InverseOp> inverseOpIndex = new HashMap<String,InverseOp>();
   public String operator, inverseOpA, inverseOpB;
   public boolean swapArgsA, swapArgsB;
   private InverseOp(String inputOp, String inverseAOp, boolean swapA, String inverseBOp, boolean swapB) {
      operator = inputOp;
      inverseOpA = inverseAOp;
      inverseOpB = inverseBOp;
      swapArgsA = swapA;
      swapArgsB = swapB;
   }
   static {
      for (InverseOp op:InverseOp.values())
         inverseOpIndex.put(op.operator, op);
   }
   public static InverseOp get(String operator) {
      return inverseOpIndex.get(operator);
   }
}

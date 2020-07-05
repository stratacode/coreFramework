import sc.util.JSON;

class UploadResult {
   Object result;
   String error;
   // TODO: add errorCode
   UploadResult(Object result, String error) {
      this.result = result;
      this.error = error;
   }

   public StringBuilder getResultJSON() {
      StringBuilder sb = new StringBuilder();
      sb.append("{\"result\":");
      sb.append(JSON.toJSON(result));
      sb.append(", ");
      sb.append("\"error\":");
      sb.append(JSON.toJSON(error));
      sb.append("}");
      return sb;
   }
}
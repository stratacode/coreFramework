package sc.lang;

@sc.obj.Sync(onDemand=true)
public class ModelError {
   @sc.obj.Sync
   @Bindable
   public int startIndex;
   @sc.obj.Sync
   @Bindable
   public int endIndex;
   @sc.obj.Sync
   @Bindable
   public String error;

   @sc.obj.Sync
   @Bindable
   public boolean notFound;

   public ModelError() {
   }

   public ModelError(String error, int startIx, int endIx, boolean notFound) {
      this.error = error;
      this.startIndex = startIx;
      this.endIndex = endIx;
      this.notFound = notFound;
   }
}

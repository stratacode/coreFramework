package sc.lang;

import java.util.List;

@sc.obj.Sync(onDemand=true)
public class CompletionResult {
   public int completeStart;
   public List<String> suggestions;
}

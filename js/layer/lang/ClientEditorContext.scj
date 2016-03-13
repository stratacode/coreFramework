import sc.bind.Bindable;
import sc.bind.Bind;
import sc.lang.java.BodyTypeDeclaration;
import sc.lang.java.JavaModel;
import sc.layer.Layer;
import sc.layer.LayeredSystem;
import sc.layer.SrcEntry;

import java.util.ArrayList;

import java.util.*;

/** This is the part of the editor context we share on the client. It's a workaround for the fact that we don't depend on the modify operator to build SC (so the IDE can build it directly) */
@sc.obj.Sync(onDemand=true)
public class ClientEditorContext {
   LayeredSystem system;

   JavaModel pendingModel = null;
   LinkedHashSet<JavaModel> changedModels = new LinkedHashSet<JavaModel>();
   LinkedHashMap<SrcEntry, Object> errorModels = new LinkedHashMap<SrcEntry, Object>();

   ArrayList<BodyTypeDeclaration> currentTypes = new ArrayList<BodyTypeDeclaration>();

   boolean currentModelStale = false;

   public Layer currentLayer;

   public String layerPrefix;

   HashMap<SrcEntry,MemoryEditSession> memSessions = new HashMap<SrcEntry, MemoryEditSession>();

   private boolean memorySessionChanged = false;
   @Bindable(manual=true)
   public boolean getMemorySessionChanged() {
      return memorySessionChanged;
   }

   public void setMemorySessionChanged(boolean val) {
      memorySessionChanged = val;
      Bind.sendChangedEvent(this, "memorySessionChanged");
   }

}

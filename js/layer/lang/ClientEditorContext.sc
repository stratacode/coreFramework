import sc.bind.Bindable;
import sc.bind.Bind;
import sc.lang.java.BodyTypeDeclaration;
import sc.lang.java.JavaModel;
import sc.layer.Layer;
import sc.layer.LayeredSystem;
import sc.layer.SrcEntry;

import java.util.ArrayList;

import java.util.*;

import sc.obj.Constant;
import sc.obj.Sync;
import sc.obj.SyncMode;

import sc.dyn.DynUtil;

import sc.type.CTypeUtil;

/** This is the part of the editor context we share on the client. It's a workaround for the fact that we don't depend on the modify operator to build SC (so the IDE can build it directly) */
@Sync(onDemand=true)
public class ClientEditorContext {
   @Constant
   public LayeredSystem system;

   JavaModel pendingModel = null;
   LinkedHashSet<JavaModel> changedModels = new LinkedHashSet<JavaModel>();
   LinkedHashMap<SrcEntry, List<ModelError>> errorModels = new LinkedHashMap<SrcEntry, List<ModelError>>();

   ArrayList<BodyTypeDeclaration> currentTypes = new ArrayList<BodyTypeDeclaration>();

   @Sync(syncMode=SyncMode.Disabled)
   boolean currentModelStale = false;

   public Layer currentLayer;

   public List<Layer> currentLayers = new ArrayList<Layer>(1);

   @Sync(syncMode=SyncMode.Disabled)
   public String layerPrefix;

   @Bindable(manual=true)
   private HashMap<SrcEntry,MemoryEditSession> memSessions = null;

   public void setMemSessions(HashMap<SrcEntry,MemoryEditSession> msMap) {
      this.memSessions = msMap;
      Bind.sendChangedEvent("this", "memSessions");
   }

   public HashMap<SrcEntry,MemoryEditSession> getMemSessions() {
      return this.memSessions;
   }

   private List<String> createInstTypeNames;

   private boolean memorySessionChanged = false;
   @Bindable(manual=true)
   public boolean getMemorySessionChanged() {
      return memorySessionChanged;
   }

   public void setMemorySessionChanged(boolean val) {
      memorySessionChanged = val;
      Bind.sendChangedEvent(this, "memorySessionChanged");
   }

   @Bindable(manual=true)
   public List<String> getCreateInstTypeNames() {
      return createInstTypeNames;
   }

   public void setCreateInstTypeNames(List<String> nl) {
      createInstTypeNames = nl;
      Bind.sendChangedEvent(this, "createInstTypeNames");
   }

   public boolean isCreateInstType(String typeName) {
      return createInstTypeNames != null && createInstTypeNames.contains(typeName);
   }

   public String getCreateInstFullTypeName(String typeName) {
      for (String tn:createInstTypeNames)
         if (CTypeUtil.getClassName(tn).equals(typeName))
            return tn;
      return null;
   }
   public void updateCurrentLayer(Layer l) {
      changeCurrentLayer(l);
      currentLayers = l.getSelectedLayers();
   }

   @sc.obj.ManualGetSet
   private void changeCurrentLayer(Layer l) {
      currentLayer = l;
   }

   public boolean hasAnyMemoryEditSession(boolean memorySessionChanged) {
      return memorySessionChanged || hasMemSessionDiffs();
   }

   public boolean hasMemSessionDiffs() {
      if (memSessions == null)
         return false;
      for (MemoryEditSession sess:memSessions.values()) {
         if (sess.text == null)
            continue;
         if (!DynUtil.equalObjects(sess.text, sess.origText))
            return true;
      }
      return false;
   }

   /** Returns the model text to display - the extra modelText param is here for data binding purposes */
   public String getModelText(JavaModel model, String modelText) {
      MemoryEditSession mes = getMemorySession(model.getSrcFile());
      if (mes == null || mes.text == null)
         return modelText;
      return mes.text;
   }

   public String getMemoryEditSessionText(SrcEntry ent) {
      MemoryEditSession mes = memSessions == null ? null : memSessions.get(ent);
      if (mes == null)
         return null;
      return mes.text;
   }

   public int getMemoryEditCaretPosition(SrcEntry ent) {
      MemoryEditSession mes = memSessions == null ? null : memSessions.get(ent);
      if (mes == null)
         return -1;
      return mes.caretPosition;
   }

   public void setMemoryEditCaretPosition(JavaModel model, int cp) {
      SrcEntry ent = model.srcFile;
      MemoryEditSession mes = memSessions == null ? null : memSessions.get(ent);
      HashMap<SrcEntry, MemoryEditSession> newMemSessions = null;
      if (mes == null) {
         mes = new MemoryEditSession();
         newMemSessions = new HashMap<SrcEntry,MemoryEditSession>();
         if (memSessions != null)
            newMemSessions.putAll(memSessions);
         newMemSessions.put(ent, mes);
      }
      mes.model = model;
      mes.caretPosition = cp;

      if (newMemSessions != null) {
         memSessions = newMemSessions;
         Bind.sendChange(this, "memSessions", memSessions);
      }
   }

   public String getMemoryEditSessionOrigText(SrcEntry ent) {
      MemoryEditSession mes = memSessions == null ? null : memSessions.get(ent);
      if (mes == null)
         return null;
      return mes.origText;
   }

   public MemoryEditSession getMemorySession(SrcEntry ent) {
      return memSessions == null ? null : memSessions.get(ent);
   }

   public void changeMemoryEditSession(String text, JavaModel model, int caretPos) {
      SrcEntry ent = model.srcFile;
      HashMap<SrcEntry,MemoryEditSession> newMemSessions = null;
      MemoryEditSession sess = null;
      if (memSessions == null)
         newMemSessions = new HashMap<SrcEntry, MemoryEditSession>();
      else
         sess = memSessions.get(ent);
      if (sess == null) {
         sess = new MemoryEditSession();
         sess.origText = model.cachedModelText;
         if (newMemSessions == null)
            newMemSessions = new HashMap<SrcEntry, MemoryEditSession>(memSessions);
         newMemSessions.put(ent, sess);
      }
      sess.text = text;
      sess.model = model;
      sess.caretPosition = caretPos;
      setMemorySessionChanged(true);
      if (newMemSessions != null) {
         memSessions = newMemSessions;
         Bind.sendChangedEvent(this, "memSessions");
      }
   }

   public void cancelMemorySessionChanges() {
      for (MemoryEditSession mes:memSessions.values()) {
         if (mes.text == null)
            continue;
         mes.text = mes.origText;
         mes.cancelled = true;
         // Trigger the refresh event in the editor... as though the text changed back to it's original value even though technically the model text did not change
         mes.model.markChanged();
      }
      // TODO: Do we have to restore any files for which we've saved something that did not update properly?
      memSessions = new HashMap<SrcEntry,MemoryEditSession>();

      setMemorySessionChanged(false);
      Bind.sendChangedEvent(this, "memSessions");
   }

   public void layerSelected(Layer l, boolean addToSelection) {
      if (currentLayers == null || currentLayers.size() == 0) {
         setCurrentLayer(l);
      }
      else if (!addToSelection) {
         if (currentLayers.get(0) == l) {
            List<Layer> newLayers = l.getSelectedLayers();
            if (!DynUtil.equalObjects(newLayers, currentLayers)) {
               currentLayers = newLayers;
               Bind.sendChangedEvent(this, "currentLayers");
            }
         }
         else {
            setCurrentLayer(l);
         }
      }
      else {
         boolean handled = false;
         int insertPos = -1;
         ArrayList<Layer> newCurrentLayers = new ArrayList<Layer>(currentLayers);
         for (int i = 0; i < currentLayers.size(); i++) {
            Layer current = currentLayers.get(i);
            if (current == l) {
               newCurrentLayers.remove(i);
               handled = true;
               break;
            }
            if (current.layerPosition < l.layerPosition) {
               insertPos = i;
               break;
            }
         }
         if (!handled) {
            if (insertPos != -1)
               newCurrentLayers.add(insertPos, l);
            else
               newCurrentLayers.add(l);
         }
         Layer newCurrentLayer = newCurrentLayers.size() == 0 ? null : newCurrentLayers.get(0);
         if (newCurrentLayer != currentLayer) {
            changeCurrentLayer(newCurrentLayer);
            Bind.sendChangedEvent(this, "currentLayer");
         }
         currentLayers = newCurrentLayers;
      }
   }

   @Bindable(manual=true)
   public void setCurrentLayer(Layer newLayer) {
      // TODO: should we try to preserve imports here and only switch the layer of the pending model if there's no type?
      if (currentLayer != newLayer) {
         updateCurrentLayer(newLayer);
         if (currentLayer != null)
            layerPrefix = currentLayer.packagePrefix;
         else
            layerPrefix = null;
         Bind.sendChangedEvent(this, "currentLayer");
         Bind.sendChangedEvent(this, "currentLayers");
      }
   }
}

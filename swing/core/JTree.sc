import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.DefaultTreeModel;

@Component
public class JTree extends javax.swing.JTree implements TextComponentStyle {
   override @Bindable location;
   override @Bindable size;
   override @Bindable visible;

   public Object rootTreeNode;
   rootTreeNode =: updateTreeModel();

   private Object lastTreeNode;

   private void updateTreeModel() {
      if (lastTreeNode == rootTreeNode)
         return;
      lastTreeNode = rootTreeNode;
      if (lastTreeNode instanceof TreeNode)
         setModel(new DefaultTreeModel((TreeNode) lastTreeNode, false));
      else if (lastTreeNode instanceof TreeModel)
         setModel((TreeModel) lastTreeNode);
      else
         setModel(createTreeModel(lastTreeNode));
   }
}

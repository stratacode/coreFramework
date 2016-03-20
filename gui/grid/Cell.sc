import java.util.Collections;


class Cell extends GridElement {
   Object value; // Stores the value assigned to the cell, either manually set, set by the row, or grid
   int colspan = 1, rowspan = 1;

   List<Object> children;

   void addChild(Object child) {
      if (children == null)
         children = new ArrayList<Object>();
      children.add(child);
   }

   int getEndRow() {
      return row + rowspan;
   }

   int getEndCol() {
      return col + colspan;
   }

   public void updateCells() {
      if (cells != null) {
         int sz;
         if ((sz = cells.size()) != rowspan || (sz > 0 && cells.get(0).size() != colspan))
            cells = null;
      }

      if (cells == null) {
         ArrayList<GridElement> cols = new ArrayList<GridElement>(colspan);
         for (int i = 0; i < colspan; i++)
            cols.add(this);

         // Note: reusing the cols array here so don't modify this in place!
         List<List<GridElement>> rows = new ArrayList<List<GridElement>>(rowspan);
         for (int i = 0; i < rowspan; i++)
            rows.add(cols);

         cells = rows;
      }
   }

   public void setDataValues(List<List<Object>> values) {
      assert values.size() == 1 && values.get(0).size() == 1;
      value = values.get(0).get(0);
   }

   public List<List<Object>> getDataValues() {
      return Collections.singletonList(Collections.singletonList(value));
   }

   /** Cells always use one value in the grid, even if colspan and rowspan are non-zero. */
   int getNumValueCols() {
      return 1;
   }

   int getNumValueRows() {
      return 1;
   }

   int getNextColCount() {
      return colspan;
   }
   
   int getNextRowCount() {
      return rowspan;
   }

   void updateLayout() {
      if (children == null)
         return;

      Alignment useAlign = resolveAlignment();
      int curX = x;
      int curY = y;

      int prefChildWidth = 0;
      int prefChildHeight = 0;
      int canShrinkW = 0, canShrinkH = -1;
      int canGrowW = 0, canGrowH = -1;
      int growW, shrinkW, growH, shrinkH;

      // First compute how growable/shrinkable things are
      for (Object child:children) {
         int childWidth = getChildPreferredWidth(child);
         int childHeight = getChildPreferredHeight(child);
         if (childWidth != -1) {
            prefChildWidth += childWidth; 
            canShrinkW += Math.abs(childWidth - getChildMinWidth(child));
            int maxWidth = getChildMaxWidth(child); 
            canGrowW += Math.abs(maxWidth - childWidth);

            prefChildHeight += childHeight;
            int newShrinkH = Math.abs(childHeight - getChildMinHeight(child));
            canShrinkH = canShrinkH == -1 ? newShrinkH : Math.min(newShrinkH, canShrinkH);
            int maxHeight = getChildMaxHeight(child);
            int newGrowH = Math.abs(maxHeight - childHeight);
            canGrowH = canGrowH == -1 ? newGrowH : Math.min(newGrowH, canGrowH);
         }
      }

      // And how much they need to grow/shrink by to meet the constraints
      if (width > prefChildWidth) {
         shrinkW = 0;
         growW = width - prefChildWidth;
      }
      else {
         shrinkW = prefChildWidth - width;
         growW = 0;
      }

      if (height > prefChildHeight) {
         shrinkH = 0;
         growH = height - prefChildHeight;
      }
      else {
         shrinkH = prefChildHeight - height;
         growH = 0;
      }

      // Growing/Shrinking children:
      //
      // If things need to be below the combined minWidths, we revert to scrolling
      // Otherwise, compute how much each cell can grow/shrink and use that ratio to determine how 
      // much it should grow/shrink

      int remainingW = width;
      int remainingH = height;
      int maxCellHeight = 0;

      int childX, childY, childW, childH;

      for (Object child:children) {
         if (child == null)
            continue;

         childX = curX;
         childY = curY;
         if (!getChildVisible(child)) {
            childW = 0;
            childH = 0;
         }
         else {
            int prefW = getChildPreferredWidth(child);
            int prefH = getChildPreferredHeight(child);
            int minW = getChildMinWidth(child);
            int minH = getChildMinHeight(child);
            int maxW = getChildMaxWidth(child);
            int maxH = getChildMaxHeight(child);

            // How do we shrink/grow horizontally?
            if (shrinkW > 0) {
               if (canShrinkW >= shrinkW) {
                  int myShrinkW = (prefW - minW) * (prefW - minW) / canShrinkW;
                  childW = prefW - myShrinkW;
               }
               else {
                  System.err.println("*** Need to enable scrolling here!");
                  childW = Math.min(minW, remainingW);
               }
            }
            else if (growW > 0) {
               if (canGrowW >= growW) {
                  // Figure out what percentage of the "can grow" amount this element makes up.   Use that ratio to scale up the grow amount
                  // so it grows smoothly and based on how much things can grow.
                  int myGrowW = 2 * (maxW - prefW) / canGrowW;
                  childW = prefW + myGrowW;
               }
               // we cannot grow enough.  
               else {
                  childW = maxW;
               }
            }
            else {
               childW = prefW;
            }

            // How do we shrink/grow vertically?
            if (shrinkH > 0) {
               if (canShrinkH >= shrinkH) {
                  int myShrinkH = (prefH - minH) * (prefH - minH) / canShrinkH;
                  childH = prefH - myShrinkH;
               }
               else {
                  System.err.println("*** Need to enable scrolling here!");
                  childH = Math.min(minH, remainingH);
               }
            }
            else if (growH > 0) {
               if (canGrowH >= growH) {
                  childH = prefH + growH;
               }
               else {
                  childH = maxH;
               }
            }
            else {
               childH = prefH;
            }

            curX += childW;
            remainingW -= childW;
            // Always on one row so no need to mess with remainingH or curY

            maxCellHeight = Math.max(maxCellHeight, childH);

            setChildDimensions(child, childX, childY, childW, childH);
         }
      }
   }

   int resolvePreferredWidth() {
      if (fixedWidth != -1)
         return fixedWidth;
      return preferredWidth == -1 ? computeChildPreferredWidth() : preferredWidth;
   }

   int resolvePreferredHeight() {
      if (fixedHeight != -1)
         return fixedHeight;
      return preferredHeight == -1 ? computeChildPreferredHeight() : preferredHeight;
   }

   private int computeChildPreferredWidth() {
      if (children == null)
         return 0;
      int sum = paddingLeft;
      for (Object child:children) {
         sum += getChildPreferredWidth(child);
      }
      sum += paddingRight;
      return sum;
   }

   private int computeChildPreferredHeight() {
      if (children == null)
         return 0;
      int sum = paddingTop;
      for (Object child:children) {
         sum += getChildPreferredHeight(child);
      }
      sum += paddingBottom;
      return sum;
   }

   private int computeChildMinWidth() {
      if (children == null)
         return 0;
      int sum = paddingLeft;
      for (Object child:children) {
         sum += getChildMinWidth(child);
      }
      sum += paddingRight;
      return sum;
   }

   private int computeChildMinHeight() {
      if (children == null)
         return 0;
      int sum = paddingTop;
      for (Object child:children) {
         sum += getChildMinHeight(child);
      }
      sum += paddingBottom;
      return sum;
   }

   private int computeChildMaxWidth() {
      if (children == null)
         return 0;
      int sum = paddingLeft;
      for (Object child:children) {
         sum += getChildMaxWidth(child);
      }
      sum += paddingRight;
      return sum;
   }

   private int computeChildMaxHeight() {
      if (children == null)
         return 0;
      int sum = paddingTop;
      for (Object child:children) {
         sum += getChildMaxHeight(child);
      }
      sum += paddingBottom;
      return sum;
   }

   int resolveMinWidth() {
      if (minWidth != -1)
         return minWidth;
      return computeChildMinWidth();
   }

   int resolveMinHeight() {
      if (minHeight != -1)
         return minHeight;
      return computeChildMaxHeight();
   }

   int resolveMaxWidth() {
      if (maxWidth != -1)
         return maxWidth;
      return MAX_CELL_WIDTH;
   }

   int resolveMaxHeight() {
      if (maxHeight != -1)
         return maxHeight;
      return MAX_CELL_HEIGHT;
   }

   /** These getChild methods are overridden in a framework specific layer. */
 
   /* Pulls the min width out of the child widget provided.  Returns -1 if there is no assigned min width or not supported by the toolkit */
   int getChildMinWidth(Object child) {
      return 10;
   }

   int getChildMaxHeight(Object child) {
      return 10;
   }

   int getChildMinHeight(Object child) {
      return 10;
   }

   int getChildMaxWidth(Object child) {
      return 10;
   }

   int getChildPreferredWidth(Object child) {
      return 10;
   }

   int getChildPreferredHeight(Object child) {
      return 10;
   }

   boolean getChildVisible(Object child) {
      return true;
   }

   void setChildDimensions(Object child, int childX, int childY, int childW, int childH) {
   }

}

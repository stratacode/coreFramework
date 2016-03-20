import java.util.BitSet;

import sc.dyn.DynUtil;

abstract class CellGroup extends GridElement {
   List<GridElement> children;

   void addChild(GridElement child) {
      if (children == null)
         children = new ArrayList<GridElement>();
      child.parent = this;
      children.add(child);
   }

   public int getEndCol() {
      if (children == null)
         return col;
      int endCol = col;
      for (GridElement child:children) {
         int childCol = child.getCol();
         if (childCol == -1) {
            childCol = endCol;
            endCol += child.getNumCols();
         }
      }
      return endCol;
   }

   public int getEndRow() {
      if (children == null)
         return row;
      int endRow = row;
      for (GridElement child:children) {
         int childRow = child.getRow();
         if (childRow == -1) {
            childRow = endRow;
            endRow += child.getNumRows();
         }
      }
      return endCol;
   }

   public void updateCells() {
      int useNumRows = numRows;
      int useNumCols = numCols;

      List<List<Object>> useVals = getDataValues();

      boolean autoRows = false, autoCols = false;
      if (useNumRows == -1 && useVals != null) {
         useNumRows = useVals.size();
         autoRows = true;
      }
      if (useNumCols == -1 && useVals != null) {
         useNumCols = getMaxColumnSize(useVals);
         autoCols = true;
      }

      if (useNumCols == -1) // Looks for the max endCol of our children
         useNumCols = getEndCol();
      if (useNumRows == -1)
         useNumRows = getEndRow();

      ArrayList<GridElement> removeCheckList = null;

      if (cells == null)
         cells = createGrid(useNumCols, useNumRows);
      else // expand the grid size if necessary right up front based on what we know
         expandGridSize(useNumCols, useNumRows, false);

      // First clear the inUse flag on all cells.  Gets set to true for all new cell types, so we can 
      // detect when we're replacing a cell already defined here.
      for (List<GridElement> colList:cells) {
         for (GridElement elem:colList) {
            if (elem != null)
               elem.inUse = false;
         }
      }

      if (children != null) {
         int curRow = getRow();
         int curCol = getCol();
         for (GridElement child:children) {
            int row = child.getRow();
            int col = child.getCol();
            if (row == -1 || child.inheritRow) {
               child.row = row = curRow;
               child.inheritRow = true;
            }
            if (col == -1 || child.inheritCol) {
               child.col = col = curCol;
               child.inheritCol = true;
            }
            List<List<GridElement>> childCells = child.getCells();
            for (int r = row; r < child.getEndRow(); r++) {
               List<GridElement> childRow = childCells.get(r-row);
               List<GridElement> cellRow = cells.get(r);
               for (int c = col; c < child.getEndCol(); c++) {
                  GridElement oldCell = cellRow.get(c);
                  GridElement newCell = childRow.get(c-col);
                  if (oldCell != newCell) {
                     if (oldCell.inUse) {
                        System.err.println("Two definitions for the same cell: " + oldCell + " and " + newCell);
                     }
                     else {
                        newCell.refCount++;
                        newCell.inUse = true;
                     }
                     cellRow.set(c, newCell);
                     if (oldCell != null) {
                        oldCell.refCount--;
                        if (removeCheckList == null) removeCheckList = new ArrayList<GridElement>();
                        removeCheckList.add(oldCell);
                     }
                  }
               }
            }
         }
      }

      int curDataCol, curDataRow = 0;


      BitSet colConsumed = new BitSet(useNumCols);
      List<GridElement> colList, prevRow = null;

      // Process any repeat rows using the current grid size.  If there's data, figure out how big we
      // need to expand the grid by.  We then expand the grid, again processing the repeat cells when we
      // do that.
      for (int r = 0; r < cells.size(); r++) {
         colList = cells.get(r);
         GridElement prevElem = null, prevRowElem = null;
         boolean rowConsumed = false;
         curDataCol = 0;
         for (int c = 0; c < colList.size(); ) {
            GridElement elem = colList.get(c);
            prevRowElem = prevRow != null ? prevRow.get(c) : null;
            if (elem == null || !elem.inUse) {
               if (prevRowElem != null && prevRowElem.repeatRow) {
                  removeCheckList = updateCellType(colList, c, prevRowElem, removeCheckList);
               }
               if (prevElem != null && prevElem.repeatCol) {
                  removeCheckList = updateCellType(colList, c, prevElem, removeCheckList);
               }
            }
            prevElem = elem;
            // If the cell is consuming data
            if (elem != null) {
                if (useVals != null && elem.useParentData && 
                  // And we are not doing a rowspan - where this cell is duplicated
                  (prevRow == null || prevRow.get(r) != elem)) {
                  rowConsumed = true;
                  colConsumed.set(curDataCol, true);
                  curDataCol += elem.getNumValueCols();
               }
               // Skips any colspan columns
               c += elem.getNextColCount();
            }
            else
               c++;
         }
         prevRow = colList;
         // If this row did not use any data, it is a header row and skips the data.
         if (!rowConsumed && autoRows)
            useNumRows++;
      }

      if (useVals != null) {
         // Count the number of columns defined which did not consume data.  Pad out the grid size so that it is large enough to accomodate 
         // those columns when we do the repeat nodes.  This would be for separator or other columns which don't consume part of the data.
         if (autoCols) {
            int toAdd = 0;
            for (int i = 0; i < useNumCols; i++)
               if (!colConsumed.get(i))
                  toAdd++;
            useNumCols += toAdd;
         }

         // May need to expand the grid size to account for header and other rows/cells 
         expandGridSize(useNumCols, useNumRows, true);

         /* Propagate the values throughout the grid. */ 
         List<GridElement> lastRow = null;
         for (int r = 0; r < cells.size(); r++) {
            colList = cells.get(r);
            curDataCol = 0;
            int maxRowsConsumed = 0;
            for (int c = 0; c < colList.size(); ) {
               GridElement elem = colList.get(c);
               if (elem != null) {
                  // Skip elements which do not use data or cells part of a rowspan, where the value is duplicated
                  if (elem.useParentData && (lastRow == null || lastRow.get(c) != elem)) {
                     if (elem instanceof Cell) {
                        ((Cell) elem).value = useVals.get(curDataRow).get(curDataCol);
                        curDataCol++;
                     }
                     else {
                        elem.setDataValues(getValuesRange(useVals, curDataCol, curDataCol + elem.getNumValueCols(), curDataRow, curDataRow + elem.getNumValueRows()));
                        curDataCol += elem.getNumValueCols();
                     }
                     if (elem.getNumValueRows() > maxRowsConsumed)
                        maxRowsConsumed = elem.getNumValueRows();
                  }
                  c += elem.getNextColCount();
               }
               else
                  c++;
            }
            curDataRow += maxRowsConsumed;
            // TODO: fix this warning
            if (curDataCol != useVals.get(curDataRow).size())
               System.out.println("*** Warning: grid size too small for data");
         }
         // TODO: and fix this one
         if (curDataRow != useVals.size())
            System.out.println("*** Warning: data grid did not consume all rows in the data");
      }

      // Now that we've got the final grid size, trim away any cells when the grid shrinks
      removeCheckList = trimGridSize(useNumCols, useNumRows, removeCheckList);

      // Any cells which have been removed from the grid might need to be culled.
      if (removeCheckList != null) {
         for (GridElement toCheck:removeCheckList) {
            if (toCheck.refCount == 0)
               removeChild(toCheck);
         }
      }
   }

   public void expandGridSize(int newNumCols, int newNumRows, boolean repeatTypes) {
      int oldNumRows = cells.size();
      int oldNumCols = cells.size() == 0 ? 0 : cells.get(0).size();
      if (newNumCols > oldNumCols) {
         List<GridElement> prevRow = null;
         for (int i = 0; i < oldNumRows; i++) {
            List<GridElement> oldRow = cells.get(i);
            int sz; 
            GridElement prevElem, prevRowElem;
            while ((sz = oldRow.size()) < newNumCols) {
               prevElem = prevRow == null || sz == 0 ? null : prevRow.get(sz-1);
               prevRowElem = prevRow == null ? null : prevRow.get(sz);
               oldRow.add(null);
               if (repeatTypes) {
                  if (prevRowElem != null && prevRowElem.repeatRow) {
                     // This should only set the type, never replace a type since we are inserting a null new element
                     updateCellType(oldRow, sz, prevRowElem, null);
                  }
                  if (prevElem != null && prevElem.repeatCol) {
                     updateCellType(oldRow, sz, prevElem, null);
                  }
               }
            }
            prevRow = oldRow;
         }
      }
      while (newNumRows > oldNumRows) {
         cells.add(blankRow(newNumCols));
      }
   }

   public ArrayList<GridElement> trimGridSize(int newNumCols, int newNumRows, ArrayList<GridElement> removeCheckList) {
      int oldNumRows = cells.size();
      int oldNumCols = cells.size() == 0 ? 0 : cells.get(0).size();
      if (oldNumCols > newNumCols) {
         for (int i = 0; i < oldNumRows; i++) {
            List<GridElement> oldRow = cells.get(i);
            int rowSize;
            while ((rowSize = oldRow.size()) > newNumCols) {
               GridElement toRemove = oldRow.remove(rowSize - 1);
               if (toRemove != null) {
                  toRemove.refCount--;
                  if (removeCheckList == null)
                     removeCheckList = new ArrayList<GridElement>();
                  removeCheckList.add(toRemove);
               }
            }
         }
      }
      while (oldNumRows > newNumRows) {
         removeCheckList = removeRow(oldNumRows-1, removeCheckList);
         oldNumRows = cells.size();
      }
      return removeCheckList;
   }

   private ArrayList<GridElement> updateCellType(List<GridElement> colList, int c, GridElement cloneElem, ArrayList<GridElement> removeCheckList) {
      GridElement elem = colList.get(c);
      Object newCellType = DynUtil.getType(cloneElem);
      if (elem == null || DynUtil.getType(elem) != newCellType) {
         GridElement newElem = (GridElement) DynUtil.createInstance(newCellType, null);
         newElem.refCount++;
         newElem.inUse = true;
         colList.set(c, newElem);
         if (elem != null) {
            if (elem.inUse) 
               System.err.println("Error: duplicate definition for cell: " + elem + " and " + cloneElem);
            elem.refCount--;
            if (removeCheckList == null) removeCheckList = new ArrayList<GridElement>();
            removeCheckList.add(elem);
         }
      }
      return removeCheckList;
   }

   private ArrayList<GridElement> removeRow(int rowIndex, ArrayList<GridElement> removeCheckList) {
      List<GridElement> oldRow = cells.remove(rowIndex);
      for (int i = 0; i < oldRow.size(); i++) {
         GridElement toRemove = oldRow.get(i);
         if (toRemove != null) {
            toRemove.refCount--;
            if (removeCheckList == null)
               removeCheckList = new ArrayList<GridElement>();
            removeCheckList.add(toRemove);
         }
      }
      return removeCheckList;
   }

   void updateLayout() {
      updateCells();

      int curX, curY = resolveY();
      int maxWidth = resolveMaxWidth();
      int maxHeight = resolveMaxHeight();

      List<GridElement> lastRow;
      for (int r = 0; r < cells.size(); r++) {
         curX = resolveX();
         List<GridElement> curRow = cells.get(r);
         int numCols = curRow.size();

         // Compute how much our children can grow or shrink as a group
         int prefChildWidth = 0;
         int prefChildHeight = 0;
         int canShrinkW = 0, canShrinkH = -1;
         int canGrowW = 0, canGrowH = -1;
         for (int c = 0; c < numCols; c++) {
            GridElement cell = curRow.get(c);
            int childWidth = cell.resolvePreferredWidth();
            int childHeight = cell.resolvePreferredHeight();
            if (childWidth != -1) {
               prefChildWidth += childWidth; 
               canShrinkW += Math.abs(childWidth - cell.resolveMinWidth());
               canGrowW += Math.abs(cell.resolveMaxWidth() - childWidth);

               prefChildHeight += childHeight;
               int newShrinkH = Math.abs(childHeight - cell.resolveMinHeight());
               canShrinkH = canShrinkH == -1 ? newShrinkH : Math.min(newShrinkH, canShrinkH);
               int newGrowH = Math.abs(cell.resolveMaxHeight() - childHeight);
               canGrowH = canGrowH == -1 ? newGrowH : Math.min(newGrowH, canGrowH);
            }
         }

         // Compute the Actual pixel amounts we need to shrink or grow
         int growW = 0, shrinkW, growH = 0, shrinkH;

         if (maxWidth == -1 || maxWidth > prefChildWidth) {
            shrinkW = 0;
            if (maxWidth == fixedWidth && maxWidth != -1) {
               growW = maxWidth - prefChildWidth;
            }
            else {
               width = prefChildWidth;
            }
         }
         else {
            shrinkW = prefChildWidth - maxWidth;
            prefChildWidth = maxWidth;
         }

         if (maxHeight == -1 || maxHeight > prefChildHeight) {
            shrinkH = 0;
            if (maxHeight == fixedHeight && maxHeight != -1) {
               growH = maxHeight - prefChildHeight;
            }
            else {
               height = prefChildHeight;
            }
         }
         else {
            shrinkH = prefChildHeight - maxHeight;
            prefChildHeight = maxHeight;
         }


         // Shrinking children:
         //
         // strategy 1:  if (sum(prefW - minW) > shrinkW)
         //        compute % (preferredW - minWidth)/sum(prefW - minW).  subtract that %*(prefW - minW) from preferredW from each cell  
         // strategy 2: enable scrolling: everyone gets minWidth;

         int remainingW = prefChildWidth;
         int remainingH = prefChildHeight;
         int maxCellHeight = 0;

         for (int c = 0; c < numCols; c++) {
            GridElement cell = curRow.get(c);

            if (cell == null)
               continue;

            cell.x = curX;
            cell.y = curY;
            if (!cell.visible) {
               cell.width = cell.height = 0;
            }
            else {
               int prefW = cell.resolvePreferredWidth();
               int prefH = cell.resolvePreferredHeight();
               int minW = cell.resolveMinWidth();
               int minH = cell.resolveMinHeight();
               int maxW = cell.resolveMaxWidth();
               int maxH = cell.resolveMaxHeight();
               if (shrinkW > 0) {
                  if (canShrinkW >= shrinkW) {
                     int myShrinkW = (prefW - minW) * (prefW - minW) / canShrinkW;
                     cell.width = prefW - myShrinkW;
                  }
                  else {
                     System.err.println("*** Need to enable horizontal scrolling here!");
                     cell.width = Math.min(minW, remainingW);
                  }
               }
               else if (growW > 0) {
                  if (canGrowW >= growW) {
                     int myGrowW = (maxWidth - prefW) * (maxWidth - prefW) / canGrowW;
                     cell.width = prefW + myGrowW;
                  }
                  else {
                     cell.width = maxW;
                  }
               }
               else {
                  cell.width = prefW;
               }

               if (shrinkH > 0) {
                  if (canShrinkH >= shrinkH) {
                     cell.height = prefH - shrinkH;
                  }
                  else {
                     System.err.println("*** Need to enable vertical scrolling here!");
                     cell.height = Math.min(minH, remainingH);
                  }
               }
               else if (growH > 0) {
                  if (maxH >= growH) {
                     cell.height = growH;
                  }
                  else {
                     cell.height = maxH;
                  }
               }
               else {
                  cell.height = prefH;
               }

               curX += cell.width;
               remainingW -= cell.width;

               maxCellHeight = Math.max(maxCellHeight, cell.height);

               cell.updateLayout();
            }
         }
         lastRow = curRow;
         curY += maxCellHeight;
      }
   }
}

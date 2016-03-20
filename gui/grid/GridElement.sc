import sc.dyn.DynUtil;

/** 
 * Could be a column, row, or individual cell definition.  Elements may repeat horizontally or vertically. 
 * You can set optional properties: fixed size, preferredSize, maxSize.  An element may get a reference to the
 * data set referred to by its parent object or it may specify its own unique data (e.g. for say a header row).
 */
abstract class GridElement {
   protected static final int MAX_CELL_WIDTH = 5000; // Pick some arbitrary maximum size for cells that will not cause overflows
   protected static final int MAX_CELL_HEIGHT = 5000; 

   CellGroup parent;

   int col = -1, row = -1;  // start point of this element in the parent element

   int refCount; // number of times this element is referenced in a parent element
   boolean inUse;  // marker flag used to tag items during refresh to detect overlaps

   protected boolean inheritRow, inheritCol;  // After we set row/col from the parent, this flag goes to true so we know we could/should reset them

   int getCol() {
      return col;
   }
   int getRow() {
      return row;
   }
   abstract int getEndCol();
   abstract int getEndRow();

   int getNumCols() {
      return getEndCol() - getCol();
   }

   int getNumRows() {
      return getEndRow() - getRow();
   }

   int getNumValueCols() {
      return getNumCols();
   }

   int getNumValueRows() {
      return getNumRows();
   }

   /** For colspan, we actually skip the duplicated cells in some cases */
   public int getNextColCount() {
      return 1;
   }

   public int getNextRowCount() {
      return 1;
   }


   List<List<GridElement>> cells;
   
   abstract void setDataValues(List<List<Object>> values);

   abstract List<List<Object>> getDataValues();

   public List<List<GridElement>> getCells() {
      if (cells == null)
         updateCells();
      return cells;
   }

   CellGroup getRoot() {
      if (parent == null)
         return null;

      while (parent.parent != null)
         parent = parent.parent;

      return parent;
   }

   int x = -1, y = -1; // Pixel position relative to the parent, assigned by the system
   int width = -1, height = -1;  // Assigned width and height to the cell.

   int fixedWidth = -1, fixedHeight = -1; // Manually ask for a fixed dimensions (min/max/preferred=fixed)
   int preferredWidth = -1, preferredHeight = -1; // Size that fits content best
   int minWidth = -1, minHeight = -1;  // Smallest size that fits content
   int maxWidth = -1, maxHeight = -1;  // Largest size for content
   int paddingTop, paddingBottom, paddingLeft, paddingRight;
   boolean visible;

   Alignment align = Alignment.Inherit;
   // TODO: add valign

   // When true, and there are blank cells after this element, expand this cell to fill those blank cells.
   boolean repeatCol, repeatRow;

   // When set, restricts the number of times the element is repeated.
   int repeatColCount = -1, repeatRowCount = -1;

   // For header rows or padding cells, set this to false.  The Row, Cell, Column then skips this cell or row when assigning data values.
   boolean useParentData = true;

   /** Updates the cells grid based on changes made to children or child properties. */
   abstract void updateCells();

   /** Updates the x, y, width, and height properties of the cell based on its parent */
   abstract void updateLayout();

   protected static int getMaxColumnSize(List<List<Object>> values) {
      int rowSize = values.size();
      int colCount = 0;
      for (int i = 0; i < rowSize; i++) {
         int newSz = values.get(i).size();
         if (i == 0 || newSz > colCount) {
            colCount = newSz;
         }
      }
      return colCount;
   }

   protected static List<List<GridElement>> createGrid(int numCols, int numRows) {
      List<List<GridElement>> grid = new ArrayList<List<GridElement>>(numRows);
      for (int i = 0; i < numRows; i++) {
         grid.add(blankRow(numCols));
      }
      return grid;
   }

   static List<GridElement> blankRow(int numCols) {
      ArrayList<GridElement> newRow = new ArrayList<GridElement>(numCols);
      for (int i = 0; i < numCols; i++)
         newRow.add(null);
      return newRow;
   }

   static List<List<Object>> getValuesRange(List<List<Object>> values, int minCol, int maxCol, int minRow, int maxRow) {
      int newRowCt = maxRow - minRow;
      // TODO: could improve the efficiency here with a new data structure that made no copy of even the rows
      ArrayList<List<Object>> res = new ArrayList<List<Object>>(newRowCt);
      for (int i = 0; i < newRowCt; i++) {
         res.add(values.get(minRow + i).subList(minCol, maxCol));
      }
      return res;
   }

   /** Called when a child cell, row, etc is removed from the active cell grid.  Hook for UIs to delete the widgets for all children etc. */
   void removeChild(GridElement toRemove) {
      DynUtil.dispose(toRemove);
   }

   int resolvePreferredWidth() {
      if (fixedWidth != -1)
         return fixedWidth;
      return preferredWidth == -1 ? (minWidth == -1 ? maxWidth : minWidth) : preferredWidth;
   }

   int resolvePreferredHeight() {
      if (fixedHeight != -1)
         return fixedHeight;
      return preferredHeight == -1 ? (minHeight == -1 ? maxHeight : minHeight) : preferredHeight;
   }

   int resolveMinWidth() {
      if (minWidth != -1)
         return minWidth;
      return resolvePreferredWidth();
   }

   int resolveMinHeight() {
      if (minHeight != -1)
         return minHeight;
      return resolvePreferredHeight();
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

   Alignment resolveAlignment() {
      return align == Alignment.Inherit ? parent.resolveAlignment() : align;
   }

   int resolveX() {
      if (x == -1) {
         if (parent == null)
            return 0;
         System.err.println("*** Attempt to access position before parent has been initialized");
         parent.updateLayout();
      }
      return x;
   }

   int resolveY() {
      if (y == -1) {
         if (parent == null)
            return 0;
         System.err.println("*** Attempt to access position before parent has been initialized");
         parent.updateLayout();
      }
      return y;
   }
}


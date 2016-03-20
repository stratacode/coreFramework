
/** 
 * Organizes its children into a definition of a grid, optionally based on a 2D data set, the values property.  
 * Children can be Rows, Columns, and Cells.  Rows and Columns organize Cells into patterns.  Cells can be repeating - so they expand
 * to fill empty space based on input data.  Cells can use absolute positions - set col, row properties manually, or they can be 
 * organized into rows and columns, which set these properties automatically.  Rows and columns can similarly be set manually via col/row or 
 * positioned relative to where they live in the list of children in their parent grid.  
 * <p>
 * The useParentData property controls whether any element consumes data from the input grid.  If not, the next cell or row gets those data
 * values.
 */
class DataGrid extends CellGroup {
   List<List<Object>> values;
   int numRows = -1, numCols = -1;

   public void setDataValues(List<List<Object>> newValues) {
      values = newValues;
   }

   public List<List<Object>> getDataValues() {
      return values;
   }
}

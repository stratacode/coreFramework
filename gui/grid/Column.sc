import java.util.Collections;

class Column extends LinearCellGroup {
   int getEndCol() {
      return col + 1;
   }

   void setDataValues(List<List<Object>> newValues) {
      int nrows = newValues.size();
      values = new ArrayList<Object>(nrows);
      for (int i = 0; i < nrows; i++) {
         List<Object> currentRow = newValues.get(i);
         assert currentRow.size() == 1;
         values.add(currentRow.get(0));
      }
   }

   public List<List<Object>> getDataValues() {
      if (values == null)
         return null;
      int sz = values.size();
      ArrayList<List<Object>> toReturn = new ArrayList<List<Object>>(sz);
      for (int i = 0; i < sz; i++) {
         toReturn.add(Collections.singletonList(values.get(i)));
      }
      return toReturn;
   }
}

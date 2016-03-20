import java.util.Collections;

class Row extends LinearCellGroup {
   int getEndRow() {
      return row + 1;
   }

   void setDataValues(List<List<Object>> newValues) {
      assert newValues.size() == 1;
      values = newValues.get(0);
   }

   public List<List<Object>> getDataValues() {
      return Collections.singletonList(values);
   }
}


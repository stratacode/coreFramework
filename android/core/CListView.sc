class CListView extends ListView {
   {
      setOnItemSelectedListener(new ListSelectListener(getOnItemSelectedListener(), this));
   }
}

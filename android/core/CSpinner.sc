class CSpinner extends Spinner {
   {
      setOnItemSelectedListener(new ListSelectListener(getOnItemSelectedListener(), this));
   }
}

import sc.js.JSSettings;

@JSSettings(prefixAlias="sc_")
public enum BindingDirection
{
   FORWARD, REVERSE, BIDIRECTIONAL, NONE;

   public boolean doForward()
   {
      switch (this)
      {
         case FORWARD:
         case BIDIRECTIONAL:
            return true;
         case REVERSE:
            return false;
      }
      return false;
   }

   public boolean doReverse()
   {
      switch (this)
      {
         case FORWARD:
            return false;
         case BIDIRECTIONAL:
         case REVERSE:
            return true;
      }
      return false;
   }

   public String getOperatorString() {
      switch (this) {
         case FORWARD:
            return ":=";
         case BIDIRECTIONAL:
            return ":=:";
         case REVERSE:
            return "=:";
      }
      return null;
   }

}

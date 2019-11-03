import sc.obj.GetSet;
abstract class Statement extends Definition {
   @GetSet
   String comment;
   Object enclosingType;
}

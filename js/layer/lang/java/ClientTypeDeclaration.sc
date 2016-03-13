class ClientTypeDeclaration extends TypeDeclaration implements sc.bind.IChangeable, sc.obj.IObjectId {
   BodyTypeDeclaration getOriginal() {
      return this;
   }

   public String getObjectId() {
      // Unique ids - MD = "metadata"
      return sc.dyn.DynUtil.getObjectId(this, null, "MD_" + typeName);
   }
}

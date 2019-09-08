@sc.obj.CompilerSettings(exportProperties=false)
JComponent {
  /* TODO: should we have a way to add bindability to subclasses to avoid having @Bindable size/location in each subclass and possibly
     to allow automatic suppression of warnings. This allows us to bind against JComponent references but requires all sub-classes
     to implement bindable events for size and location for it to work properly.
   */
  override @Bindable(manual=true) size;
  override @Bindable(manual=true) location;
}

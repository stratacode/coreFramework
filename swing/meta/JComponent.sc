JComponent {
/* TODO: should we have a way to add bindability to subclasses to avoid having @Bindable size in each subclass and possibly
   to allow automatic suppression of warnings
   */
  override @Bindable(manual=true) size;
  override @Bindable(manual=true) location;
}

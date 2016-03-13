PanelStyle {
  // No need to specify an initializer to add a property to a layer.  It will inherit the initializer in this case.  
  override xpad;
  ypad = 4;
  baseline = 5;
  background := ComponentStyle.defaultBackground;
  override border;
}

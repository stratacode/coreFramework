@Component
@CompilerSettings(// object and new templates to handle passing the context parameter to the constructor
                  objectTemplate="android.view.ViewObj",
                  newTemplate="android.view.ViewNew",
                  // Auto-create constructor with this signature to call super(context) unless one is there
                  propagateConstructor="android.content.Context")
View {}

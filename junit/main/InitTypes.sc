// This will inject code to init all startup types so that it's loaded before we start running tests
@CompilerSettings(mixinTemplate="sc.type.InitTypesMixin")
public class InitTypes {
}

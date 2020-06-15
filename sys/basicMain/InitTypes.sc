// This will inject code to init all startup types so that it's loaded before we start running tests
@CompilerSettings(mixinTemplate="sc.type.InitTypesMixin")
class InitTypes extends sc.type.InitTypesBase {
   @sc.obj.MainSettings
   static void main(String[] args) {
       InitTypes it = new InitTypes();
       it.initTypes();
   }
}

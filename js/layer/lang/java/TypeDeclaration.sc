import java.util.List;
import sc.layer.Layer;

class TypeDeclaration extends BodyTypeDeclaration {
   // We should only ever allocate ClientTypeDeclaration on the client because of the
   // sync handler on the server.  The server needs to wrap the type declaration object
   // in the client type declaration wrapper because the type declaration class itself gets
   // treated as a 'class' and not an object.
   ClientTypeDeclaration getClientTypeDeclaration() {
      return (ClientTypeDeclaration) this;
   }
}

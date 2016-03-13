import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
  * Use the URL annotation to mark a class which corresponds to a URL.  If you do not specify a pattern, the default pattern is
  * the type name of the file with the .html suffix.  So a type "Foo" would be fetched with /Foo.html.  If you do specify a pattern,
  * that pattern is used for the URL in ths page type.  When a URL matching that pattern is entered, the request maps to that page object.
  * <p>
  * It can be set on a top-level page, using the default page=true attribute actually handle the request.  In this case you set @URL on a Servlet or
  * page which has an schtml file associated with it.  For schtml files, the @URL is set by framework layers on the html tag by default so you do not
  * have to use @URL at all, unless you want to change the URL for a given object.
  * <p>
  * With page=true, the object for that page is used to generate the output for the request.  The first URL which matches a page completes the request
  * and you are finished.  When page=false, each component that matches the URL is populated with data values extracted from the URL.
  (TODO: each component?  right now the code is only doing the first component to match the URL)
  */
@Target({ElementType.TYPE})
// Keep these at runtime so we can do the getURLPaths lookup at runtime using the compiled class, without having to retrieve the source.
@Retention(RetentionPolicy.RUNTIME)
public @interface URL {
   /** Either a simple string like "/index.html" that defines the URL, or it can be a pattern string to match URLs with data in them.
       The pattern string lets you match any pattern defined in the SC language, using the name of the parselet behind that language element.
       For example the pattern:   @URL(pattern="/bookTitle/{integerLiteral}") would match: /bookTitle/12345.   You can also set properties in your object by specifying
       variable names like: @URL(pattern="/bookTitle/{bookId=integerLiteral}")   When your page is called, it's bookId property will always be set to a valid integer.
       */
   String pattern() default "";
   /** When page is true, the URL matches a top-level page object which handle the request.  When it is false, the first component */
   boolean page() default true;
   /** CSS files that are generated dynamically are resources and not included in the index */
   boolean resource() default false;
   /** Set this to true so that a base type is not itself asigned a URL but all sub-types in the chain are assigned the URL.  If you want to bury
    the @URL annotation on a subclass, so it's not set on each individual class, using this option avoids having an entry for the abstract base class. */
   boolean subTypesOnly() default false;
   /** Set to the scope name to be used for locking purposes in the PageDispatcher and Sync requests.  Defaults to the scope of the instance if not set.  Set to none to disable locking.  */
   String lockScope() default "";
}

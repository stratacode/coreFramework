import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
  * Use the URL annotation on a class or object to have it automatically handle URL requests for both dynamic content and static resources.
  * The pattern attribute for the annotation can be a simple string to match (e.g. "/login"), or a URLPattern (e.g. /blog/{post=urlString}).
  * The default value for pattern if one is not specified is to use the type name of the file with a '.html' suffix.
  * So by default, a type "Foo" would be fetched with /Foo.html.
  * <p>
  * Set dynContent=true for dynamic HTML and false for requests that serve images, plain css files or other static resources. In a production environment,
  * we will code-generate the mapping for a front-end web server to handle those request (TODO).
  * Setting dynContent=false will skip the locking and other context initialization and allow for simple pass-through servlet pages. It will also allow
  * the content of those requests to be cached by the browser.
  * <p>
  * With dynContent=true, the object for that page is used to generate the output for the request.  The first URL which matches a page completes the request
  * and you are finished.
  * <p>
  * For dynContent=true pages, use the @QueryParam annotation on properties of the type with the URL to have those values auto-populated when the page is rendered, and
  * for the URL to be updated on the client when the properties are changed. Similarly, properties specified in the URLPattern will be set in the page instance
  * before the request is handled.
  */
@Target({ElementType.TYPE})
// Keep these at runtime so we can do the getURLPaths lookup at runtime using the compiled class, without having to retrieve the source.
@Retention(RetentionPolicy.RUNTIME)
public @interface URL {
   /**
    * Either a simple string like "/index.html" that defines the URL, or it can be a pattern string to match URLs with data in them.
    * The pattern string is defined using the sc.lang.PatternLanguage (as used from the sc.lang.pattern.Pattern class). This lets you produce a string
    * with embedded values parsed from other languages.  In this case, PageDispatcher lets you use any public parselet in the SCLanguage as variable types.
    * For example the pattern:   @URL(pattern="/bookTitle/{integer}") would match: /bookTitle/12345.   You can also set properties in your object by specifying
    * variable names like: @URL(pattern="/bookTitle/{bookId=integer}")   When your page is called, it's bookId property will always be set to a valid integer.
    */
   String pattern() default "";
   /** For requests to static resources, set this to false to disable locking, setting of URL/query params into the instance, and enabling of the cache headers */
   boolean dynContent() default true;
   /** Set to true for css, images or other files. If resources are dynamic, the app id is chosen from the referer(sic) header */
   boolean resource() default false;
   /** Set this to true so that a base type is not itself asigned a URL but all sub-types in the chain are assigned the URL.  If you want to bury
    the @URL annotation on a subclass, so it's not set on each individual class, using this option avoids having an entry for the abstract base class. */
   boolean subTypesOnly() default false;
   /** Set to the scope name to be used for locking purposes in the PageDispatcher and Sync requests.  Defaults to the scope of the instance if not set.  Set to none to disable locking.  */
   String lockScope() default "";

   /** Should realtime be enabled/disabled for this URL */
   boolean realTime() default true;

   /** Set to a list of URLs to be added to the set which are run for the automatic tests */
   String[] testURLs() default {};
}

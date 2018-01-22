import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
/**
 * Set this on a field, get or setX method for a property you want to associate with a query param in the request URL.
 * This must be set on an immediate field of an object which has the @URL annotation.   Set required = true if the
 * page should only be loaded if the query param is present.
 */
public @interface QueryParam {
   String name() default ""; // The query parameter name - only required if different from the property name.
   boolean required() default false;
}

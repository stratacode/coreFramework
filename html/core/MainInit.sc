import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface MainInit {
   String name() default "";
   String jsFile() default ""; // Default is to add this type and dependent types to sc.js the default
   boolean enabled() default true;
   boolean subTypesOnly() default false;  // Set this to true so that a base type is not itself instantiated, but all sub-types in the chain are
}

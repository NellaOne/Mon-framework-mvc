package mg.etu3273.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation personnalisée pour marquer les classes contrôleur
 * Équivalent du @Controller de Spring
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {
    
    /**
     * Nom du contrôleur (optionnel)
     */
    String value() default "";
}
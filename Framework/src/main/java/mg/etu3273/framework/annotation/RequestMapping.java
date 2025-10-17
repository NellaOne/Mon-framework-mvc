package mg.etu3273.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation personnalisée pour mapper les méthodes aux URLs
 * Équivalent du @RequestMapping de Spring
 * 
 * Utilisation:
 * @RequestMapping("/dept/liste")
 * public String liste() { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {
    
    /**
     * L'URL à mapper à cette méthode
     * Exemple: "/dept/liste", "/users/add"
     */
    String value();
    
    /**
     * Méthodes HTTP supportées (optionnel)
     * Par défaut: GET et POST
     */
    String[] method() default {"GET", "POST"};
}
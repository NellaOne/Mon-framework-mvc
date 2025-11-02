package mg.etu3273.framework;

import java.lang.reflect.Method;

/**
 * Classe pour stocker le mapping entre une URL et une méthode de contrôleur
 * Sprint 2 bis
 */
public class Mapping {
    private String url;           // L'URL mappée (ex: "/test/list")
    private String className;     // Nom complet de la classe (ex: "mg.etu3273.test.Test2")
    private Method method;        // La méthode Java correspondante
    
    // Constructeur vide
    public Mapping() {
    }
    
    // Constructeur avec paramètres
    public Mapping(String url, String className, Method method) {
        this.url = url;
        this.className = className;
        this.method = method;
    }
    
    // Getters et Setters
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public Method getMethod() {
        return method;
    }
    
    public void setMethod(Method method) {
        this.method = method;
    }
    
    @Override
    public String toString() {
        return "Mapping{" +
                "url='" + url + '\'' +
                ", className='" + className + '\'' +
                ", method=" + (method != null ? method.getName() : "null") +
                '}';
    }
}
package mg.etu3273.framework.scanner;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe Mapping - Sprint 3-bis
 * Gère le mapping entre URL et méthode de contrôleur
 * Supporte maintenant les URLs avec paramètres dynamiques : /dept/{id}
 */
public class Mapping {
    
    private String url;           // L'URL mappée (ex: "/dept/{id}")
    private String className;     // Nom complet de la classe
    private Method method;        // La méthode Java correspondante
    
    // ✅ SPRINT 3-bis : Nouveau pour gérer les URLs dynamiques
    private boolean hasDynamicParams;  // true si l'URL contient {}
    private String urlPattern;         // Pattern regex pour matcher l'URL
    private List<String> paramNames;   // Noms des paramètres (ex: ["id"])
    
    // Constructeur par défaut
    public Mapping() {
        this.paramNames = new ArrayList<>();
    }
    
    // Constructeur avec paramètres
    public Mapping(String url, String className, Method method) {
        this.url = url;
        this.className = className;
        this.method = method;
        this.paramNames = new ArrayList<>();
        
        // ✅ Analyse de l'URL pour détecter les paramètres dynamiques
        analyzeUrl();
    }
    
    /**
     * ✅ SPRINT 3-bis - Analyse l'URL pour détecter les {} et créer le pattern regex
     */
    private void analyzeUrl() {
        if (url == null) {
            this.hasDynamicParams = false;
            return;
        }
        
        // Vérifier si l'URL contient des paramètres dynamiques {}
        this.hasDynamicParams = url.contains("{") && url.contains("}");
        
        if (this.hasDynamicParams) {
            // Extraire les noms des paramètres
            Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
            Matcher matcher = pattern.matcher(url);
            
            while (matcher.find()) {
                String paramName = matcher.group(1);
                paramNames.add(paramName);
            }
            
            // Créer le pattern regex pour matcher l'URL
            // Ex: /dept/{id} devient /dept/([^/]+)
            this.urlPattern = url.replaceAll("\\{[^}]+\\}", "([^/]+)");
            
            System.out.println("   🔧 URL dynamique détectée: " + url);
            System.out.println("      Pattern regex: " + urlPattern);
            System.out.println("      Paramètres: " + paramNames);
        } else {
            // URL statique normale
            this.urlPattern = url;
        }
    }
    
    /**
     * ✅ SPRINT 3-bis - Vérifie si une URL demandée correspond à ce mapping
     * @param requestedUrl L'URL demandée (ex: "/dept/17")
     * @return true si l'URL correspond
     */
    public boolean matches(String requestedUrl) {
        if (!hasDynamicParams) {
            // URL statique : comparaison directe
            return url.equals(requestedUrl);
        } else {
            // URL dynamique : utilisation de regex
            Pattern pattern = Pattern.compile("^" + urlPattern + "$");
            Matcher matcher = pattern.matcher(requestedUrl);
            return matcher.matches();
        }
    }
    
    /**
     * ✅ SPRINT 3-bis - Extrait les valeurs des paramètres depuis l'URL
     * Ex: URL pattern "/dept/{id}", URL demandée "/dept/17" → ["17"]
     * Note: Implémentation complète dans Sprint 6-ter
     */
    public List<String> extractParamValues(String requestedUrl) {
        List<String> values = new ArrayList<>();
        
        if (!hasDynamicParams) {
            return values;
        }
        
        Pattern pattern = Pattern.compile("^" + urlPattern + "$");
        Matcher matcher = pattern.matcher(requestedUrl);
        
        if (matcher.matches()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                values.add(matcher.group(i));
            }
        }
        
        return values;
    }
    
    // Getters et Setters
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
        analyzeUrl(); // Ré-analyser si l'URL change
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
    
    public boolean hasDynamicParams() {
        return hasDynamicParams;
    }
    
    public String getUrlPattern() {
        return urlPattern;
    }
    
    public List<String> getParamNames() {
        return paramNames;
    }
    
    @Override
    public String toString() {
        if (hasDynamicParams) {
            return "Mapping{url='" + url + "' (dynamique), classe=" + className + 
                   ", methode=" + method.getName() + ", params=" + paramNames + "}";
        } else {
            return "Mapping{url='" + url + "', classe=" + className + 
                   ", methode=" + method.getName() + "}";
        }
    }
}
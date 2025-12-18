package mg.etu3273.framework.scanner;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

public class Mapping {
    
    private String url;          
    private String className;     
    private Method method;    
    private String httpMethod;     
    private boolean hasDynamicParams;  
    private String urlPattern;        
    private List<String> paramNames;  
    
    public Mapping() {
        this.paramNames = new ArrayList<>();
    }
    
    public Mapping(String url, String className, Method method) {
        this.url = url;
        this.className = className;
        this.method = method;
        this.paramNames = new ArrayList<>();
        analyzeUrl();
    }
    
    public Mapping(String url, String className, Method method, String httpMethod) {
        this.url = url;
        this.className = className;
        this.method = method;
        this.httpMethod = httpMethod;
        this.paramNames = new ArrayList<>();
        analyzeUrl();
    }

    private void analyzeUrl() {
        if (url == null) {
            this.hasDynamicParams = false;
            return;
        }
        
        this.hasDynamicParams = url.contains("{") && url.contains("}");
        
        if (this.hasDynamicParams) {
            Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
            Matcher matcher = pattern.matcher(url);
            
            while (matcher.find()) {
                String paramName = matcher.group(1);
                paramNames.add(paramName);
            }
            
            this.urlPattern = url.replaceAll("\\{[^}]+\\}", "([^/]+)");
        } else {
            this.urlPattern = url;
        }
    }
    
    public boolean matches(String requestedUrl) {
        if (!hasDynamicParams) {
            return url.equals(requestedUrl);
        } else {
            Pattern pattern = Pattern.compile("^" + urlPattern + "$");
            Matcher matcher = pattern.matcher(requestedUrl);
            return matcher.matches();
        }
    }

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

    public boolean supportsHttpMethod(String requestMethod) {
        if (this.httpMethod == null) {
            return true;
        }
        return this.httpMethod.equalsIgnoreCase(requestMethod);
    }

    public static Mapping findMapping(String requestedUrl, String httpMethod, Map<String, List<Mapping>> urlMappings) {
        List<Mapping> candidates = urlMappings.get(requestedUrl);
        if (candidates != null) {
            for (Mapping m : candidates) {
                if (m.supportsHttpMethod(httpMethod)) {
                    return m;
                }
            }
            return null;
        }

        System.out.println("🔍 Recherche de match dynamique pour: " + requestedUrl);
        
        for (Map.Entry<String, List<Mapping>> entry : urlMappings.entrySet()) {
            for (Mapping mapping : entry.getValue()) {
                if (mapping.hasDynamicParams() && mapping.matches(requestedUrl)) {
                    if (mapping.supportsHttpMethod(httpMethod)) {                    
                        List<String> values = mapping.extractParamValues(requestedUrl);
                        if (!values.isEmpty()) {
                            System.out.println("   Valeurs extraites: " + values);

                            if (mapping.validateUrlParamTypes(values)) {
                                 System.out.println("   ✅ Types compatibles");
                                 return mapping;
                            } else  {
                                System.out.println("   ❌ Types incompatibles, recherche d'une autre route...");
                                continue;
                            }
                        }
                        return mapping;
                    }
                }
            }
        }
        System.out.println("❌ Aucun mapping trouvé pour: " + requestedUrl);
        return null;
    }

    public boolean validateUrlParamTypes(List<String> urlParamValues) {
        Parameter[] parameters = method.getParameters();
        List<String> urlParamNames = getParamNames();
        
        for (int i = 0; i < urlParamNames.size(); i++) {
            if (i >= urlParamValues.size()) {
                continue; // Pas assez de valeurs
            }
            
            String paramName = urlParamNames.get(i);
            String paramValue = urlParamValues.get(i);
            
            // Trouver le type attendu de ce paramètre dans la méthode
            Class<?> expectedType = null;
            for (Parameter param : parameters) {
                if (param.getName().equals(paramName)) {
                    expectedType = param.getType();
                    break;
                }
            }
            
            if (expectedType != null) {
                // Tester la conversion
                Object converted = convertParameter(paramValue, expectedType);
                if (converted == null && paramValue != null && !paramValue.isEmpty()) {
                    System.out.println("      ⚠️ Valeur '" + paramValue + "' incompatible avec " + expectedType.getSimpleName());
                    return false; // Conversion impossible
                }
            }
        }
        return true;
    }


    public Object[] prepareMethodArguments(HttpServletRequest request, String requestedUrl) {
        Parameter[] parameters = method.getParameters();
        
        if (parameters.length == 0) {
            return new Object[0];
        }

        System.out.println("   🔧 Injection des paramètres:");

        List<String> urlParamNames = getParamNames();
        List<String> urlParamValues = extractParamValues(requestedUrl);
        
        // Récupérer TOUS les paramètres HTTP disponibles
        Map<String, String[]> allHttpParams = request.getParameterMap();
        System.out.println("      📋 Paramètres HTTP disponibles: " + allHttpParams.keySet());
        
        // Identifier les préfixes utilisés par les objets
        List<String> usedPrefixes = new ArrayList<>();
        for (Parameter param : parameters) {
            if (isCustomObject(param.getType())) {
                usedPrefixes.add(param.getName());
            }
        }
        System.out.println("      🏷️ Préfixes réservés pour objets: " + usedPrefixes);
        
        Object[] args = new Object[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType(); 
            String paramName = param.getName();

            // SPRINT 8 : Map<String, Object>
            if (isRequestParametersMap(param)) {
                args[i] = buildParametersMap(request);
                System.out.println("      ✅ " + paramName + " Map<String, Object> = " + 
                                 ((Map<?, ?>)args[i]).size() + " paramètre(s) (SPRINT 8)");
                continue;
            }

            // SPRINT 8-bis : Objets personnalisés
            if (isCustomObject(paramType)) {
                args[i] = bindObject(paramType, paramName, request);
                System.out.println("      ✅ " + paramName + " (" + paramType.getSimpleName() + ") = objet bindé (SPRINT 8-bis)");
                continue;
            }

            // === PARAMÈTRES SIMPLES (String, Integer, etc.) ===
            String paramValue = null;
            String source = "";
            
            // Sprint 6-bis : @RequestParam
            if (param.isAnnotationPresent(mg.etu3273.framework.annotation.RequestParam.class)) {
                mg.etu3273.framework.annotation.RequestParam requestParam = 
                    param.getAnnotation(mg.etu3273.framework.annotation.RequestParam.class);
                String httpParamName = requestParam.value(); 
                paramValue = request.getParameter(httpParamName);
                source = "@RequestParam(\"" + httpParamName + "\")";
            }
            // Sprint 6-ter : URL dynamique {}
            else if (urlParamNames.contains(paramName)) {
                int paramIndex = urlParamNames.indexOf(paramName);
                if (paramIndex < urlParamValues.size()) {
                    paramValue = urlParamValues.get(paramIndex);
                    source = "URL {" + paramName + "}";
                }
            }
            // Sprint 6 : Paramètre HTTP normal
            else {
                // Essayer d'abord avec le nom du paramètre Java
                paramValue = request.getParameter(paramName);
                source = "HTTP param '" + paramName + "'";
                
                // Si pas trouvé (car compilé sans -parameters)
                // Chercher un paramètre HTTP qui n'appartient à aucun objet
                if (paramValue == null) {
                    for (String httpParamName : allHttpParams.keySet()) {
                         /* if (!httpParamName.contains(".") && !usedPrefixes.contains(httpParamName)) { 
                            paramValue = request.getParameter(httpParamName);
                            source = "HTTP param '" + httpParamName + "' (auto-détecté)";
                            break;
                         } */
                        // Ignorer les paramètres avec "." (ils appartiennent à des objets)
                        if (httpParamName.contains(".")) {
                            continue;
                        } 
                        
                        // Vérifier que ce paramètre n'est pas un préfixe d'objet
                        boolean isObjectPrefix = false;
                        for (String prefix : usedPrefixes) {
                            if (httpParamName.equals(prefix)) {
                                isObjectPrefix = true;
                                break;
                            }
                        }
                        
                        if (!isObjectPrefix) {
                            String[] values = allHttpParams.get(httpParamName);
                            if (values != null && values.length > 0) {
                                paramValue = values[0];
                                source = "HTTP param '" + httpParamName + "' (auto-détecté)";
                                System.out.println("      🔍 Auto-détection: " + httpParamName + " = " + paramValue);
                                break;
                            }
                        }
                    }
                }
            }
            
            if (paramValue != null) {
                args[i] = convertParameter(paramValue, paramType);
                System.out.println("      ✅ " + paramName + " (" + paramType.getSimpleName() + ") = " + args[i] + " (depuis " + source + ")");
            } else {
                args[i] = null;
                System.out.println("      ⚠️ " + paramName + " (" + paramType.getSimpleName() + ") = null");
            }
        }
        
        return args;
    }

    // ==================== SPRINT 8-bis : MÉTHODES DE BINDING ====================

    /**
     * Vérifie si c'est un objet personnalisé (Employee, Department, etc.)
     */
    private boolean isCustomObject(Class<?> clazz) {
        // Types primitifs et wrappers
        if (clazz.isPrimitive() || 
            clazz == String.class ||
            clazz == Integer.class ||
            clazz == Long.class ||
            clazz == Double.class ||
            clazz == Float.class ||
            clazz == Boolean.class ||
            clazz == Character.class ||
            clazz == Byte.class ||
            clazz == Short.class) {
            return false;
        }
        
        // Map, List, Set, etc.
        if (Map.class.isAssignableFrom(clazz) ||
            java.util.Collection.class.isAssignableFrom(clazz)) {
            return false;
        }
        
        // Classes Java standard
        String packageName = clazz.getPackage() != null ? clazz.getPackage().getName() : "";
        if (packageName.startsWith("java.") || 
            packageName.startsWith("javax.") ||
            packageName.startsWith("jakarta.")) {
            return false;
        }
        
        return true;
    }

    /**
     * Crée et remplit un objet depuis la requête HTTP
     */
    private Object bindObject(Class<?> clazz, String prefix, HttpServletRequest request) {
        try {
            System.out.println("         🔨 Binding: " + clazz.getSimpleName() + " (prefix: " + prefix + ")");
            
            Object instance = clazz.getDeclaredConstructor().newInstance();
            
            java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
            
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                
                String fieldName = field.getName();
                String fullFieldName = prefix + "." + fieldName;
                Class<?> fieldType = field.getType();
                
                // Objet imbriqué (e.department)
                if (isCustomObject(fieldType)) {
                    System.out.println("            🔗 Objet imbriqué: " + fieldName + " (" + fieldType.getSimpleName() + ")");
                    Object nestedObject = bindObject(fieldType, fullFieldName, request);
                    
                    // Ne setter que si au moins un champ a été rempli
                    if (hasAtLeastOneField(nestedObject)) {
                        field.set(instance, nestedObject);
                    }
                }
                // Liste/Tableau
                else if (java.util.List.class.isAssignableFrom(fieldType) || fieldType.isArray()) {
                    System.out.println("            📋 Liste/Tableau: " + fieldName);
                    Object listValue = bindList(fieldType, fullFieldName, request);
                    if (listValue != null) {
                        field.set(instance, listValue);
                    }
                }
                // Attribut simple
                else {
                    String paramValue = request.getParameter(fullFieldName);
                    if (paramValue != null && !paramValue.trim().isEmpty()) {
                        Object convertedValue = convertParameter(paramValue, fieldType);
                        field.set(instance, convertedValue);
                        System.out.println("            ✅ " + fullFieldName + " = " + convertedValue);
                    }
                }
            }
            
            return instance;
            
        } catch (Exception e) {
            System.out.println("         ❌ Erreur binding: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Vérifie si au moins un champ de l'objet est non-null
     */
    private boolean hasAtLeastOneField(Object obj) {
        if (obj == null) return false;
        
        try {
            for (java.lang.reflect.Field field : obj.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                if (field.get(obj) != null) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignorer
        }
        
        return false;
    }

    /**
     * Gère les listes/tableaux
     */
    private Object bindList(Class<?> fieldType, String fieldName, HttpServletRequest request) {
        // Méthode 1 : checkboxes (name="e.hobbies" value="Lecture")
        String[] values = request.getParameterValues(fieldName);
        
        if (values == null || values.length == 0) {
            // Méthode 2 : index (name="e.hobbies[0]", name="e.hobbies[1]")
            List<String> indexedValues = new ArrayList<>();
            int index = 0;
            while (true) {
                String indexedName = fieldName + "[" + index + "]";
                String value = request.getParameter(indexedName);
                if (value == null || value.trim().isEmpty()) break;
                indexedValues.add(value);
                index++;
            }
            
            if (!indexedValues.isEmpty()) {
                values = indexedValues.toArray(new String[0]);
            }
        }
        
        if (values != null && values.length > 0) {
            System.out.println("            ✅ " + fieldName + " = " + java.util.Arrays.toString(values));
            
            // Retourner List<String> par défaut
            if (java.util.List.class.isAssignableFrom(fieldType)) {
                return java.util.Arrays.asList(values);
            }
            // Retourner String[] si tableau demandé
            else if (fieldType.isArray()) {
                return values;
            }
        }
        
        return null;
    }

    // ==================== SPRINT 8 : MAP ====================

    private boolean isRequestParametersMap(Parameter param) {
        if (!Map.class.isAssignableFrom(param.getType())) {
            return false;
        }

        Type genericType = param.getParameterizedType();
        if (!(genericType instanceof ParameterizedType)) {
            return false;
        }

        ParameterizedType parameterizedType = (ParameterizedType) genericType;
        Type[] typeArguments = parameterizedType.getActualTypeArguments();

        if (typeArguments.length != 2) {
            return false;
        }

        boolean keyIsString = typeArguments[0].equals(String.class);
        boolean valueIsObject = typeArguments[1].equals(Object.class);

        return keyIsString && valueIsObject;
    }
    
    private Map<String, Object> buildParametersMap(HttpServletRequest request) {
        Map<String, Object> parametersMap = new HashMap<>();
        
        Map<String, String[]> parameterMap = request.getParameterMap();
        
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String paramName = entry.getKey();
            String[] paramValues = entry.getValue();
            
            if (paramValues == null || paramValues.length == 0) {
                parametersMap.put(paramName, null);
            } else if (paramValues.length == 1) {
                parametersMap.put(paramName, paramValues[0]);
            } else {
                parametersMap.put(paramName, paramValues);
            }
        }
        
        System.out.println("      📦 Map construite: " + parametersMap.keySet());
        
        return parametersMap;
    }

    // ==================== CONVERSION ====================

    private Object convertParameter(String value, Class<?> targetType) {
        if (value == null || value.trim().isEmpty()) return null;
        
        try {
            if (targetType == String.class) return value;
            if (targetType == Integer.class || targetType == int.class) return Integer.valueOf(value);
            if (targetType == Long.class || targetType == long.class) return Long.valueOf(value);
            if (targetType == Double.class || targetType == double.class) return Double.valueOf(value);
            if (targetType == Float.class || targetType == float.class) return Float.valueOf(value);
            if (targetType == Boolean.class || targetType == boolean.class) return Boolean.valueOf(value);
            
            return value;
            
        } catch (NumberFormatException e) {
            System.out.println("      ❌ Erreur conversion: " + value + " vers " + targetType.getSimpleName());
            return null;
        }
    }

    // ==================== GETTERS / SETTERS ====================

    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
        analyzeUrl(); 
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

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
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
        StringBuilder sb = new StringBuilder();
        sb.append("Mapping{");
        sb.append("url='").append(url).append("'");
        
        if (hasDynamicParams) {
            sb.append(" (dynamique, params=").append(paramNames).append(")");
        }
        
        if (httpMethod != null) {
            sb.append(", httpMethod=").append(httpMethod);
        } else {
            sb.append(", httpMethod=ALL");
        }
        
        sb.append(", classe=").append(className);
        sb.append(", methode=").append(method.getName());
        sb.append("}");
        
        return sb.toString();
    }
}
package mg.etu3273.framework.scanner;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
        this.httpMethod = null;
        
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
            
            System.out.println("   🔧 URL dynamique détectée: " + url);
            System.out.println("      Pattern regex: " + urlPattern);
            System.out.println("      Paramètres: " + paramNames);
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
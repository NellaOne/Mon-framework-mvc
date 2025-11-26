package mg.etu3273.framework;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.etu3273.framework.scanner.Mapping;
import mg.etu3273.framework.scanner.PackageScanner;

public class FrontServlet extends HttpServlet {    
    private static final String URL_MAPPINGS_KEY = "framework.urlMappings";
    private RequestDispatcher defaultDispatcher;

    @Override
    public void init() throws ServletException {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");
        try {
            Map<String,  List<Mapping>> urlMappings = PackageScanner.scanAllClasspath();
            ServletContext context = getServletContext();
            context.setAttribute(URL_MAPPINGS_KEY, urlMappings);
        } catch (Exception e) {
            throw new ServletException("Erreur lors du scan des controlleurs", e);
        }
    }
    
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String httpMethod = request.getMethod(); 
        /* if (path.equals("/") || path.isEmpty()) {
            handleMvcRequest(request, response);
            return;
        } */ 
        boolean resourceExists = getServletContext().getResource(path) != null;
        if (resourceExists) {
            defaultDispatcher.forward(request, response);
            return;
        }

         @SuppressWarnings("unchecked")
        Map<String, List<Mapping>> urlMappings = (Map<String, List<Mapping>>) 
            getServletContext().getAttribute(URL_MAPPINGS_KEY);
        
        if (urlMappings == null) {
            throw new ServletException("URL Mappings non initialisé dans ServletContext");
        }
        
        Mapping mapping = findMapping(path, httpMethod, urlMappings);

        if (mapping != null) {
            handleControllerMethod(request, response, mapping, path);
        } else {
            handle404(request, response, path, httpMethod, urlMappings);
        }
    }
    
    private Object[] prepareMethodArguments(Method method, HttpServletRequest request, 
                                           Mapping mapping, String requestedUrl) {
        Parameter[] parameters = method.getParameters();
        
        if (parameters.length == 0) {
            return new Object[0];
        }

        List<String> urlParamNames = mapping.getParamNames();
        List<String> urlParamValues = mapping.extractParamValues(requestedUrl);
        
        Object[] args = new Object[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            String paramName = param.getName(); 
            Class<?> paramType = param.getType(); 

            // String httpParamName = paramName; 
            String paramValue = null;
            String source = "";
            
            if (param.isAnnotationPresent(mg.etu3273.framework.annotation.RequestParam.class)) {
                mg.etu3273.framework.annotation.RequestParam requestParam = 
                    param.getAnnotation(mg.etu3273.framework.annotation.RequestParam.class);
                String httpParamName = requestParam.value(); 
                paramValue = request.getParameter(httpParamName);
                // source = "@RequestParam";
                source = "@RequestParam(\"" + httpParamName + "\")";
            }

            else if (urlParamNames.contains(paramName)) {
                int paramIndex = urlParamNames.indexOf(paramName);
                if (paramIndex < urlParamValues.size()) {
                    paramValue = urlParamValues.get(paramIndex);
                    source = "URL {" + paramName + "}";
                }
            }

            else {
                paramValue = request.getParameter(paramName);
                source = "HTTP param";
            }
            
            // String paramValue = request.getParameter(httpParamName);
            
            if (paramValue != null) {
                args[i] = convertParameter(paramValue, paramType);
                System.out.println("      ✅ " + paramName + " (" + paramType.getSimpleName() + ") = " + args[i] + " (depuis HTTP)");
            } else {
                args[i] = null;
                System.out.println("      ⚠️ " + paramName + " (" + paramType.getSimpleName() + ") = null (pas dans request)");
            }
        }
        
        return args;
    }

    private Object convertParameter(String value, Class<?> targetType) {
        if (value == null) return null;
        
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
    
    private Mapping findMapping(String requestedUrl, String httpMethod, Map<String, List<Mapping>> urlMappings) {
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
                        }

                        return mapping;
                    }
                }
            }
        }
        System.out.println("❌ Aucun mapping trouvé pour: " + requestedUrl);
        return null;
    }


    private void handleControllerMethod(HttpServletRequest request, HttpServletResponse response, Mapping mapping,
                                       String requestedUrl) throws IOException, ServletException {
        try {
            System.out.println("🎯 Exécution: " + mapping.getClassName() + "." + mapping.getMethod().getName() + "()");
            
            // ✅ SPRINT 3-bis : Afficher info sur URL dynamique
            if (mapping.hasDynamicParams()) {
                System.out.println("   📌 URL dynamique: " + mapping.getUrl());
                System.out.println("   📌 URL demandée: " + requestedUrl);
                List<String> paramValues = mapping.extractParamValues(requestedUrl);
                if (!paramValues.isEmpty()) {
                    System.out.println("   📌 Valeurs extraites: " + paramValues);
                }
            }

            Class<?> clazz = Class.forName(mapping.getClassName());
            Object controllerInstance = clazz.getDeclaredConstructor().newInstance();

            Method method = mapping.getMethod();

            Object[] methodArgs = prepareMethodArguments(method, request, mapping, requestedUrl);
             
            Object result = method.invoke(controllerInstance, methodArgs);
            
            
            /* Object result;

            int paramCount = method.getParameterCount();
            if (paramCount > 0) {
                System.out.println("   ⚠️ Méthode avec " + paramCount + " paramètre(s) - valeurs null (Sprint 3-bis)");
                Object[] nullParams = new Object[paramCount];
                for (int i = 0; i < paramCount; i++) {
                    nullParams[i] = null;
                }
                result = method.invoke(controllerInstance, nullParams);
            } else {
                result = method.invoke(controllerInstance);
            } */

            if (result == null) {
                sendSimpleResponse(response, "La méthode a retourné NULL");

            } else if (result instanceof String) {
                sendSimpleResponse(response, (String) result);
                
            } else if (result instanceof ModelView) {
                ModelView modelView = (ModelView) result;
                String viewPath = modelView.getView();

                if (viewPath == null || viewPath.trim().isEmpty()) {
                    throw new ServletException("ModelView.view est null ou vide");
                }
                
                 if (!viewPath.startsWith("/")) {
                    viewPath = "/" + viewPath;
                }

                Map<String, Object> data = modelView.getData();

                if (data != null && !data.isEmpty()) {
                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        request.setAttribute(key, value);
                    }
                } else {
                    System.out.println("📦 Aucune donnée à transférer");
                }
                RequestDispatcher dispatcher = request.getRequestDispatcher(viewPath);
                if (dispatcher == null) {
                    throw new ServletException("Impossible de trouver la vue: " + viewPath);
                }
                
                dispatcher.forward(request, response);
                
            } else {
                sendSimpleResponse(response, result.toString());
            }
            
        } catch(Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, e, mapping);
        }
    }
    
    private void sendSimpleResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head><meta charset='UTF-8'><title>Framework MVC</title></head>");
        out.println("<body>");
        out.println("<h1>Framework MVC</h1>");
        out.println("<p>" + message + "</p>");
        out.println("</body>");
        out.println("</html>");
    }

    private void sendErrorResponse(HttpServletResponse response, Exception e, Mapping mapping) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head><meta charset='UTF-8'><title>Erreur</title></head>");
        out.println("<body>");
        out.println("<h1>❌ Erreur</h1>");
        out.println("<p><strong>Contrôleur:</strong> " + mapping.getClassName() + "</p>");
        out.println("<p><strong>Méthode:</strong> " + mapping.getMethod().getName() + "</p>");
        out.println("<p><strong>Message:</strong> " + e.getMessage() + "</p>");
        out.println("<pre>");
        e.printStackTrace(out);
        out.println("</pre>");
        out.println("</body>");
        out.println("</html>");
    }

    private void handle404(HttpServletRequest request, 
                          HttpServletResponse response, 
                          String path, String httpMethod,
                          Map<String, List<Mapping>> urlMappings) throws IOException {
        
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head><meta charset='UTF-8'><title>404</title></head>");
        out.println("<body>");
        out.println("<h1>❌ 404 - URL non trouvée</h1>");
        out.println("<p>L'URL <strong>" + path + "</strong> n'est pas mappée.</p>");
        out.println("<h3>URLs disponibles:</h3>");
        out.println("<ul>");

        for (Map.Entry<String, List<Mapping>> entry : urlMappings.entrySet()) {
            String url = entry.getKey();
            List<Mapping> mappings = entry.getValue();

            out.println("<li>");
            out.println("<a href='" + request.getContextPath() + url + "'>" + url + "</a>");

            for (Mapping m : mappings) {
                String method = m.getHttpMethod() != null ? m.getHttpMethod() : "ALL";
                out.println("<span class='method " + method + "'>" + method + "</span>");
            }
            out.println("</li>");
        }
        
        out.println("</ul>");
        out.println("</div>");
        out.println("</body>");
        out.println("</html>");
    }

 @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        service(request, response);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        service(request, response);
    }
}
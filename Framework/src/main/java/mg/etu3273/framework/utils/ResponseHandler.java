package mg.etu3273.framework.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Field;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.etu3273.framework.ModelView;
import mg.etu3273.framework.scanner.Mapping;


public class ResponseHandler {
    public static void sendSimpleResponse(HttpServletResponse response, String message) 
            throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        out.print("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Framework MVC</title></head>");
        out.print("<body><h1>Framework MVC</h1><p>" + message + "</p></body></html>");
    }

    public static void sendErrorResponse(HttpServletResponse response, Exception e, Mapping mapping) 
            throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        out.print("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Erreur</title></head><body>");
        out.print("<h1>❌ Erreur</h1>");
        out.print("<p><strong>Contrôleur:</strong> " + mapping.getClassName() + "</p>");
        out.print("<p><strong>Méthode:</strong> " + mapping.getMethod().getName() + "</p>");
        out.print("<p><strong>Message:</strong> " + escapeHtml(e.getMessage()) + "</p>");
        out.print("<pre>");
        e.printStackTrace(out);
        out.print("</pre></body></html>");
    }

    public static void send404Response(HttpServletRequest request, 
                                      HttpServletResponse response,
                                      String path,
                                      String httpMethod,
                                      Map<String, List<Mapping>> urlMappings) 
            throws IOException {
        
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        out.print("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>404</title>");
        out.print("<style>");
        out.print("body{font-family:Arial;margin:40px;background:#f5f5f5}");
        out.print(".container{background:white;padding:30px;border-radius:10px}");
        out.print("h1{color:#e74c3c}");
        out.print(".method{display:inline-block;padding:4px 8px;border-radius:4px;margin-right:5px;font-size:12px;font-weight:bold}");
        out.print(".GET{background:#27ae60;color:white}");
        out.print(".POST{background:#3498db;color:white}");
        out.print(".ALL{background:#95a5a6;color:white}");
        out.print("</style></head><body><div class='container'>");
        
        out.print("<h1>❌ 404 - URL non trouvée</h1>");
        out.print("<p>La requête <strong>" + httpMethod + " " + escapeHtml(path) + "</strong> n'est pas mappée.</p>");
        out.print("<h3>URLs disponibles :</h3><ul>");
        
        for (Map.Entry<String, List<Mapping>> entry : urlMappings.entrySet()) {
            String url = entry.getKey();
            List<Mapping> mappings = entry.getValue();
            
            out.print("<li><a href='" + request.getContextPath() + escapeHtml(url) + "'>" + escapeHtml(url) + "</a>");
            
            for (Mapping m : mappings) {
                String method = m.getHttpMethod() != null ? m.getHttpMethod() : "ALL";
                out.print("<span class='method " + method + "'>" + method + "</span>");
            }
            
            out.print("</li>");
        }
        
        out.print("</ul></div></body></html>");
    }

    public static void forwardToView(HttpServletRequest request, HttpServletResponse response, 
                              ModelView modelView) throws ServletException, IOException {
        String viewPath = modelView.getView();
        
        if (viewPath == null || viewPath.trim().isEmpty()) {
            throw new ServletException("ModelView.view est null ou vide");
        }
        if (!viewPath.startsWith("/")) {
            viewPath = "/WEB-INF/views/" + viewPath;
        }

        Map<String, Object> data = modelView.getData();
        if (data != null && !data.isEmpty()) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                request.setAttribute(entry.getKey(), entry.getValue());
            }
        } else {
                    System.out.println("📦 Aucune donnée à transférer");
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher(viewPath);
        if (dispatcher == null) {
            throw new ServletException("Vue introuvable: " + viewPath);
        }
        dispatcher.forward(request, response);
    }
  
    public static void sendJsonResponse(HttpServletResponse response, Object result) 
            throws IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter out = response.getWriter();
        
        // Convertir l'objet en JSON
        String json = toJson(result);
        out.print(json);
        out.flush();
        
        System.out.println("📤 JSON envoyé: " + json);
    }

    public static JsonResponse prepareJsonResponse(Object result) {
        if (result == null) {
            return JsonResponse.error(500, "La méthode a retourné null");
        }
        
        // Si c'est déjà une JsonResponse, on la retourne telle quelle
        if (result instanceof JsonResponse) {
            return (JsonResponse) result;
        }
        
        // Si c'est un ModelView, extraire les données
        if (result instanceof ModelView) {
            ModelView mv = (ModelView) result;
            Map<String, Object> data = mv.getData();
            
            if (data == null || data.isEmpty()) {
                return JsonResponse.success(null);
            }
            
            // Si une seule entrée, retourner l'objet directement
            if (data.size() == 1) {
                Object value = data.values().iterator().next();
                return wrapInJsonResponse(value);
            }
            
            // Plusieurs entrées, retourner tout le Map
            return JsonResponse.success(data);
        }
        
        // Pour tous les autres types (Object, String, Integer, List, etc.)
        return wrapInJsonResponse(result);
    }

    private static JsonResponse wrapInJsonResponse(Object obj) {
        if (obj == null) {
            return JsonResponse.success(null);
        }
        
        // Si c'est une Collection ou un tableau
        if (obj instanceof Collection) {
            Collection<?> collection = (Collection<?>) obj;
            return JsonResponse.successList(collection, collection.size());
        }
        
        if (obj.getClass().isArray()) {
            Object[] array = (Object[]) obj;
            return JsonResponse.successList(array, array.length);
        }
        
        // Sinon, c'est un objet simple
        return JsonResponse.success(obj);
    }

    private static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        // Types primitifs et String
        if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        
        // Collection (List, Set, etc.)
        if (obj instanceof Collection) {
            StringBuilder sb = new StringBuilder("[");
            Collection<?> collection = (Collection<?>) obj;
            boolean first = true;
            for (Object item : collection) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        
        // Tableau
        if (obj.getClass().isArray()) {
            StringBuilder sb = new StringBuilder("[");
            Object[] array = (Object[]) obj;
            for (int i = 0; i < array.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(toJson(array[i]));
            }
            sb.append("]");
            return sb.toString();
        }
        
        // Map
        if (obj instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            Map<?, ?> map = (Map<?, ?>) obj;
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":");
                sb.append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        
        // Objet personnalisé : utiliser la réflexion
        return objectToJson(obj);
    }

    private static String objectToJson(Object obj) {
        StringBuilder sb = new StringBuilder("{");
        
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();
        
        boolean first = true;
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                
                if (!first) sb.append(",");
                
                sb.append("\"").append(field.getName()).append("\":");
                sb.append(toJson(value));
                
                first = false;
            } catch (IllegalAccessException e) {
                // Ignorer ce champ
            }
        }
        
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
}
package mg.etu3273.framework.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

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
  
    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
}
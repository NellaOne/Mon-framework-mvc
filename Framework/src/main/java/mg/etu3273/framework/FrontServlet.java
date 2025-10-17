package mg.etu3273.framework;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
/**
 * FrontServlet - Point d'entrée unique pour toutes les requêtes
 * Inspiré du DispatcherServlet de Spring MVC
 */
// @WebServlet(name = "FrontServlet", urlPatterns = {"/"}, loadOnStartup = 1)
public class FrontServlet extends HttpServlet {
    
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String path = request.getRequestURI().substring(request.getContextPath().length());
        
        
        boolean resourceExists = getServletContext().getResource(path) != null;

        
        
        // Configuration de la réponse
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        // Récupération des informations de la requête
        String contextPath = request.getContextPath();
        String servletPath = request.getServletPath();
        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();
        String method = request.getMethod();
        
        // Affichage des informations de debug
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("    <title>Framework MVC - Debug Info</title>");
        out.println("    <style>");
        out.println("        body { font-family: Arial, sans-serif; margin: 40px; background-color: #f5f5f5; }");
        out.println("        .container { background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }");
        out.println("        .header { color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px; }");
        out.println("        .info { margin: 15px 0; }");
        out.println("        .label { font-weight: bold; color: #34495e; }");
        out.println("        .value { color: #e74c3c; font-family: monospace; background: #ecf0f1; padding: 2px 6px; border-radius: 3px; }");
        out.println("        .success { color: #27ae60; font-weight: bold; }");
        out.println("    </style>");
        out.println("</head>");
        out.println("<body>");
        out.println("    <div class='container'>");
        out.println("        <h1 class='header'>🚀 Framework MVC - Sprint 1</h1>");
        out.println("        <p class='success'>✅ Votre requête est bien passée par le FrontServlet !</p>");
        out.println("        <div class='info'><span class='label'>Méthode HTTP:</span> <span class='value'>" + method + "</span></div>");
        out.println("        <div class='info'><span class='label'>Context Path:</span> <span class='value'>" + contextPath + "</span></div>");
        out.println("        <div class='info'><span class='label'>Servlet Path:</span> <span class='value'>" + servletPath + "</span></div>");
        out.println("        <div class='info'><span class='label'>Request URI:</span> <span class='value'>" + requestURI + "</span></div>");
        if (queryString != null) {
            out.println("        <div class='info'><span class='label'>Query String:</span> <span class='value'>" + queryString + "</span></div>");
        }
        out.println("        <hr>");
        out.println("        <h3>🔧 Informations techniques</h3>");
        out.println("        <div class='info'><span class='label'>Servlet:</span> <span class='value'>FrontServlet</span></div>");
        out.println("        <div class='info'><span class='label'>Framework:</span> <span class='value'>Mon Framework MVC v1.0</span></div>");
        out.println("        <div class='info'><span class='label'>Sprint:</span> <span class='value'>Sprint 1 - Configuration de base</span></div>");
        out.println("    </div>");
        out.println("</body>");
        out.println("</html>");
        
        // Log pour le développement
        System.out.println("FrontServlet - Requête reçue: " + method + " " + requestURI);
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
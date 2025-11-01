package mg.etu3273.framework;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {
    
    private RequestDispatcher defaultDispatcher;

    private Map<String, Mapping> urlMappings = new HashMap<>();

    @Override
    public void init() throws ServletException {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");

        try {
            String basePackage = getInitParameter("basePackage");
            if (basePackage == null || basePackage.isEmpty()) {
                basePackage = "mg.etu3273";
            }

            System.out.println("=== INITIALISATION FRONTSERVLET ===");
            System.out.println("Package de base: " + basePackage);

            urlMappings = PackageScanner.scanControllers(basePackage);

            System.out.println("URLs enregistrées: " + urlMappings.size());
            System.out.println("=================================");
        } catch (Exception e) {
            throw new ServletException("Erreur lors du scan des controlleurs", e);
        }
    }
    
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String path = request.getRequestURI().substring(request.getContextPath().length());
        
        System.out.println("📥 Requête reçue: " + request.getMethod() + " " + path);

        if (path.equals("/") || path.isEmpty()) {
            handleMvcRequest(request, response);
            return;
        }
        
        boolean resourceExists = getServletContext().getResource(path) != null;

        if (resourceExists) {
            System.out.println("📄 Ressource statique détectée, forward vers Tomcat");
            defaultDispatcher.forward(request, response);
            return;
        } /* else {
            handleMvcRequest(request, response);
        } */

        Mapping mapping = urlMappings.get(path);

        if (mapping != null) {
            handleControllerMethod(request, response, mapping);
        } else {
            handle404(request, response, path);
        }
    }
    

    private void handleControllerMethod(HttpServletRequest request, HttpServletResponse response, Mapping mapping) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {

            System.out.println("🎯 Exécution du contrôleur:");
            System.out.println("   Classe: " + mapping.getClassName());
            System.out.println("   Méthode: " + mapping.getMethod().getName());

            Class<?> clazz = Class.forName(mapping.getClassName());
            Object controllerInstance = clazz.getDeclaredConstructor().newInstance();

            Method method = mapping.getMethod();
            Object result = method.invoke(controllerInstance);

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("    <title>Framework MVC - Sprint 2</title>");
            out.println("    <style>");
            out.println("        body { font-family: Arial; margin: 40px; background: #f5f5f5; }");
            out.println("        .container { background: white; padding: 30px; border-radius: 8px; }");
            out.println("        .success { color: #27ae60; font-weight: bold; font-size: 20px; }");
            out.println("        .info { margin: 20px 0; padding: 15px; background: #ecf0f1; border-radius: 5px; }");
            out.println("    </style>");
            out.println("</head>");
            out.println("<body>");
            out.println("    <div class='container'>");
            out.println("        <h1>✅ Framework MVC - Sprint 2</h1>");
            out.println("        <p class='success'>URL mappée avec succès !</p>");
            out.println("        <div class='info'>");
            out.println("            <strong>URL:</strong> " + request.getRequestURI() + "<br>");
            out.println("            <strong>Contrôleur:</strong> " + mapping.getClassName() + "<br>");
            out.println("            <strong>Méthode:</strong> " + method.getName() + "<br>");
            out.println("            <strong>Résultat:</strong> " + result);
            out.println("        </div>");
            out.println("    </div>");
            out.println("</body>");
            out.println("</html>");

        } catch(Exception e) {
            System.err.println("❌ ERREUR lors de l'exécution du contrôleur: " + e.getMessage());
            e.printStackTrace();
            
            out.println("<h1>❌ Erreur lors de l'exécution du contrôleur</h1>");
            out.println("<pre>" + e.getMessage() + "</pre>");
            out.println("<pre>");
            e.printStackTrace(new PrintWriter(out));
            out.println("</pre>");
        }
    }

    private void handle404(HttpServletRequest request, HttpServletResponse response, String path) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = null;

        try {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("text/html;charset=UTF-8");
            out = response.getWriter();
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("    <title>404 - Page Non Trouvée</title>");
            out.println("    <style>");
            out.println("        body { font-family: Arial; margin: 40px; background: #f5f5f5; }");
            out.println("        .container { background: white; padding: 30px; border-radius: 8px; }");
            out.println("        .error { color: #e74c3c; font-size: 24px; }");
            out.println("    </style>");
            out.println("</head>");
            out.println("<body>");
            out.println("    <div class='container'>");
            out.println("        <h1 class='error'>❌ 404 - Page Non Trouvée</h1>");
            out.println("        <p>L'URL demandée <strong>" + path + "</strong> n'est pas mappée.</p>");
            out.println("        <p>URLs disponibles:</p>");
            out.println("        <ul>");
            for (String url : urlMappings.keySet()) {
                out.println("            <li><a href='" + request.getContextPath() + url + "'>" + url + "</a></li>");
            }   out.println("        </ul>");
            out.println("    </div>");
            out.println("</body>");
            out.println("</html>");
        } catch (IOException ex) {
        } finally {
            out.close();
        }

    }


    private void handleMvcRequest(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        String contextPath = request.getContextPath();
        String servletPath = request.getServletPath();
        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();
        String method = request.getMethod();
        
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
        
        System.out.println("FrontServlet - Requête MVC reçue: " + method + " " + requestURI);
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
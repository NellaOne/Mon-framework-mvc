package mg.etu3273.framework;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {    
    private static final String URL_MAPPINGS_KEY = "framework.urlMappings";

    private RequestDispatcher defaultDispatcher;
    // private Map<String, Mapping> urlMappings = new HashMap<>();

    @Override
    public void init() throws ServletException {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");

        try {
           /*  String basePackage = getInitParameter("basePackage");
            if (basePackage == null || basePackage.isEmpty()) {
                basePackage = "mg.etu3273";
            }
            System.out.println("=== INITIALISATION FRONTSERVLET ===");
            System.out.println("Package de base: " + basePackage); */
            // urlMappings = PackageScanner.scanControllers(basePackage);
            // urlMappings = PackageScanner.scanAllClasspath();
            System.out.println("=== INITIALISATION FRONTSERVLET ===");
            
            Map<String, Mapping> urlMappings = PackageScanner.scanAllClasspath();

            System.out.println("URLs enregistrées: " + urlMappings.size());

            ServletContext context = getServletContext();
            context.setAttribute(URL_MAPPINGS_KEY, urlMappings);
            System.out.println("✅ URL Mappings stocké dans ServletContext avec la clé: " + URL_MAPPINGS_KEY);
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
        /* if (path.equals("/") || path.isEmpty()) {
            handleMvcRequest(request, response);
            return;
        } */
        
        boolean resourceExists = getServletContext().getResource(path) != null;
        if (resourceExists) {
            System.out.println("📄 Ressource statique détectée, forward vers Tomcat");
            defaultDispatcher.forward(request, response);
            return;
        }

         @SuppressWarnings("unchecked")
        Map<String, Mapping> urlMappings = (Map<String, Mapping>) 
            getServletContext().getAttribute(URL_MAPPINGS_KEY);
        
        if (urlMappings == null) {
            throw new ServletException("URL Mappings non initialisé dans ServletContext");
        }
        
        Mapping mapping = urlMappings.get(path);

        if (mapping != null) {
            handleControllerMethod(request, response, mapping);
        } else {
            handle404(request, response, path, urlMappings);
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
            System.out.println("   ✅ Classe chargée: " + clazz.getSimpleName());

            Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
            System.out.println("   ✅ Instance créée: " + controllerInstance.getClass().getName());

            Method method = mapping.getMethod();
            System.out.println("   ✅ Méthode récupérée: " + method.getName() + "()");

            Object result = method.invoke(controllerInstance);
            System.out.println("   ✅ Méthode invoquée avec succès !");
            System.out.println("   📦 Résultat retourné: " + result);
            System.out.println("   📦 Type de résultat: " + (result != null ? result.getClass().getSimpleName() : "null"));

            displayResult(out, request, mapping, method, result);
            
        } catch(Exception e) {
            System.err.println("❌ ERREUR lors de l'invocation du contrôleur: " + e.getMessage());
            e.printStackTrace();
            
            displayError(out, e, mapping);
        }
    }
    
    private void displayResult(PrintWriter out, 
                              HttpServletRequest request,
                              Mapping mapping, 
                              Method method, 
                              Object result) {
        
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("    <meta charset='UTF-8'>");
        out.println("    <title>Framework MVC - Sprint 4</title>");
        out.println("    <style>");
        out.println("        body { font-family: 'Segoe UI', Arial; margin: 0; padding: 40px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }");
        out.println("        .container { background: white; padding: 40px; border-radius: 15px; box-shadow: 0 10px 30px rgba(0,0,0,0.3); max-width: 900px; margin: 0 auto; }");
        out.println("        h1 { color: #2c3e50; border-bottom: 3px solid #667eea; padding-bottom: 15px; margin-bottom: 30px; }");
        out.println("        .success { background: #d4edda; color: #155724; padding: 20px; border-radius: 8px; border-left: 5px solid #28a745; margin: 20px 0; }");
        out.println("        .info-grid { display: grid; grid-template-columns: 200px 1fr; gap: 15px; margin: 20px 0; }");
        out.println("        .label { font-weight: bold; color: #495057; }");
        out.println("        .value { color: #212529; background: #f8f9fa; padding: 8px 12px; border-radius: 5px; font-family: 'Courier New', monospace; }");
        out.println("        .result-box { background: #fff3cd; border: 2px solid #ffc107; padding: 20px; border-radius: 8px; margin: 20px 0; }");
        out.println("        .result-content { font-size: 18px; font-weight: bold; color: #856404; word-wrap: break-word; }");
        out.println("        .badge { display: inline-block; padding: 5px 10px; border-radius: 12px; font-size: 12px; font-weight: bold; }");
        out.println("        .badge-success { background: #28a745; color: white; }");
        out.println("        .badge-info { background: #17a2b8; color: white; }");
        out.println("        .section { margin: 30px 0; }");
        out.println("    </style>");
        out.println("</head>");
        out.println("<body>");
        out.println("    <div class='container'>");
        out.println("        <h1>🚀 Framework MVC - Sprint 4</h1>");
        out.println("        <div class='success'>");
        out.println("            <h2 style='margin: 0 0 10px 0;'>✅ Méthode invoquée avec succès !</h2>");
        out.println("            <p style='margin: 0;'>La méthode du contrôleur a été exécutée via Reflection</p>");
        out.println("        </div>");
        
        out.println("        <div class='section'>");
        out.println("            <h3>📋 Informations de Mapping</h3>");
        out.println("            <div class='info-grid'>");
        out.println("                <div class='label'>URL demandée:</div>");
        out.println("                <div class='value'>" + request.getRequestURI() + "</div>");
        out.println("                <div class='label'>Contrôleur:</div>");
        out.println("                <div class='value'>" + mapping.getClassName() + "</div>");
        out.println("                <div class='label'>Méthode invoquée:</div>");
        out.println("                <div class='value'>" + method.getName() + "()</div>");
        out.println("                <div class='label'>Type de retour:</div>");
        out.println("                <div class='value'>" + method.getReturnType().getSimpleName() + "</div>");
        out.println("            </div>");
        out.println("        </div>");
        
        out.println("        <div class='section'>");
        out.println("            <h3>📦 Résultat de l'Invocation</h3>");
        out.println("            <div class='result-box'>");
        
        if (result == null) {
            out.println("                <div class='result-content'>⚠️ La méthode a retourné NULL</div>");
        } else if (result instanceof String) {
            out.println("                <span class='badge badge-success'>String</span>");
            out.println("                <div class='result-content' style='margin-top: 10px;'>");
            out.println("                    " + result);
            out.println("                </div>");
        } else {
            out.println("                <span class='badge badge-info'>" + result.getClass().getSimpleName() + "</span>");
            out.println("                <div class='result-content' style='margin-top: 10px;'>");
            out.println("                    " + result.toString());
            out.println("                </div>");
        }
        
        out.println("            </div>");
        out.println("        </div>");
        
        out.println("        <div class='section'>");
        out.println("            <h3>✅ Vérifications Sprint 4</h3>");
        out.println("            <ul>");
        out.println("                <li>✅ Classe chargée dynamiquement</li>");
        out.println("                <li>✅ Instance du contrôleur créée</li>");
        out.println("                <li>✅ Méthode invoquée via Reflection</li>");
        out.println("                <li>✅ Valeur de retour récupérée</li>");
        out.println("                <li>✅ Résultat affiché avec PrintWriter</li>");
        out.println("            </ul>");
        out.println("        </div>");
        
        out.println("        <div style='margin-top: 30px; padding: 15px; background: #e7f3ff; border-radius: 8px;'>");
        out.println("            <p style='margin: 0; color: #004085;'><strong>🎯 Sprint 4 Complété:</strong> Invocation réussie via Reflection !</p>");
        out.println("        </div>");
        
        out.println("    </div>");
        out.println("</body>");
        out.println("</html>");
    }

    private void displayError(PrintWriter out, Exception e, Mapping mapping) {
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("    <meta charset='UTF-8'>");
        out.println("    <title>Erreur - Framework MVC</title>");
        out.println("    <style>");
        out.println("        body { font-family: Arial; margin: 40px; background: #f5f5f5; }");
        out.println("        .container { background: white; padding: 30px; border-radius: 8px; }");
        out.println("        .error { color: #e74c3c; }");
        out.println("        pre { background: #f8f9fa; padding: 15px; border-radius: 5px; overflow-x: auto; }");
        out.println("    </style>");
        out.println("</head>");
        out.println("<body>");
        out.println("    <div class='container'>");
        out.println("        <h1 class='error'>❌ Erreur lors de l'invocation</h1>");
        out.println("        <p><strong>Contrôleur:</strong> " + mapping.getClassName() + "</p>");
        out.println("        <p><strong>Méthode:</strong> " + mapping.getMethod().getName() + "</p>");
        out.println("        <p><strong>Message d'erreur:</strong></p>");
        out.println("        <pre>" + e.getMessage() + "</pre>");
        out.println("        <p><strong>Stack trace:</strong></p>");
        out.println("        <pre>");
        e.printStackTrace(out);
        out.println("        </pre>");
        out.println("    </div>");
        out.println("</body>");
        out.println("</html>");
    }

    private void handle404(HttpServletRequest request, 
                          HttpServletResponse response, 
                          String path,
                          Map<String, Mapping> urlMappings) {

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = null;
        
        try {
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
                Mapping m = urlMappings.get(url);
                out.println("            <li>");
                out.println("                <a href='" + request.getContextPath() + url + "'>" + url + "</a>");
                out.println("                → " + m.getClassName() + "." + m.getMethod().getName() + "()");
                out.println("            </li>");
            }
            out.println("        </ul>");
            out.println("    </div>");
            out.println("</body>");
            out.println("</html>");
            
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /* private void handleMvcRequest(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        @SuppressWarnings("unchecked")
        Map<String, Mapping> urlMappings = (Map<String, Mapping>) 
            getServletContext().getAttribute(URL_MAPPINGS_KEY);
        
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

        if (urlMappings != null) {
            out.println("        <div class='info'><span class='label'>URLs mappées:</span> <span class='value'>" + urlMappings.size() + "</span></div>");
            out.println("        <p><em>✅ Mappings stockés dans ServletContext</em></p>");
        }

        out.println("    </div>");
        out.println("</body>");
        out.println("</html>");
        
        System.out.println("FrontServlet - Requête MVC reçue: " + method + " " + requestURI);
    } */
    
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
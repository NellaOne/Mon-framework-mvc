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

    @Override
    public void init() throws ServletException {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");
        try {
            System.out.println("=== INITIALISATION FRONTSERVLET - SPRINT 4 ===");
            Map<String, Mapping> urlMappings = PackageScanner.scanAllClasspath();
            System.out.println("URLs enregistrées: " + urlMappings.size());
            ServletContext context = getServletContext();
            context.setAttribute(URL_MAPPINGS_KEY, urlMappings);
            System.out.println("✅ Mappings stockés dans ServletContext");
            System.out.println("=================================");
        } catch (Exception e) {
            throw new ServletException("Erreur lors du scan des controlleurs", e);
        }
    }
    
   @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        System.out.println("📥 Requête: " + request.getMethod() + " " + path);
        /* if (path.equals("/") || path.isEmpty()) {
            handleMvcRequest(request, response);
            return;
        } */ 
        boolean resourceExists = getServletContext().getResource(path) != null;
        if (resourceExists) {
            System.out.println("→ Ressource statique");
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
    

    private void handleControllerMethod(HttpServletRequest request, HttpServletResponse response, Mapping mapping) throws IOException, ServletException {
        response.setContentType("text/html;charset=UTF-8");
        try {
            System.out.println("🎯 SPRINT 4-bis - Exécution du contrôleur via Reflection:");
            Class<?> clazz = Class.forName(mapping.getClassName());
            Object controllerInstance = clazz.getDeclaredConstructor().newInstance();

            Method method = mapping.getMethod();
            Object result = method.invoke(controllerInstance);
            System.out.println("📦 Résultat: " + (result != null ? result.getClass().getSimpleName() : "null"));

            if (result == null) {
                sendSimpleResponse(response, "La méthode a retourné NULL");
             } else if (result instanceof String) {
                System.out.println("→ Affichage String");
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

                System.out.println("→ Dispatch vers: " + viewPath);
                RequestDispatcher dispatcher = request.getRequestDispatcher(viewPath);
                
                if (dispatcher == null) {
                    throw new ServletException("Impossible de trouver la vue: " + viewPath);
                }
                
                dispatcher.forward(request, response);
                
            } else {
                System.out.println("→ Affichage toString()");
                sendSimpleResponse(response, result.toString());
            }
            
        } catch(Exception e) {
            System.err.println("❌ ERREUR: " + e.getMessage());
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
                          String path,
                          Map<String, Mapping> urlMappings) throws IOException {
        
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
        for (String url : urlMappings.keySet()) {
            Mapping m = urlMappings.get(url);
            out.println("<li><a href='" + request.getContextPath() + url + "'>" + url + "</a> ");
            out.println("→ " + m.getClassName() + "." + m.getMethod().getName() + "()</li>");
        }
        out.println("</ul>");
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
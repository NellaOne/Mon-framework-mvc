package mg.etu3273.framework;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.etu3273.framework.scanner.Mapping;
import mg.etu3273.framework.scanner.PackageScanner;
import mg.etu3273.framework.utils.ResponseHandler;


public class FrontServlet extends HttpServlet {    
    private static final String URL_MAPPINGS_KEY = "framework.urlMappings";
    private RequestDispatcher defaultDispatcher;

    @Override
    public void init() throws ServletException {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");
        
        try {
            Map<String,  List<Mapping>> urlMappings = PackageScanner.scanAllClasspath();
            getServletContext().setAttribute(URL_MAPPINGS_KEY, urlMappings);
        } catch (Exception e) {
            throw new ServletException("Erreur lors du scan des controlleurs", e);
        }
    }
    
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String httpMethod = request.getMethod(); 

        if (getServletContext().getResource(path) != null) {
            defaultDispatcher.forward(request, response);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, List<Mapping>> urlMappings = (Map<String, List<Mapping>>) 
            getServletContext().getAttribute(URL_MAPPINGS_KEY);
        
        if (urlMappings == null) {
            throw new ServletException("URL Mappings non initialisé dans ServletContext");
        }
        
        Mapping mapping = Mapping.findMapping(path, httpMethod, urlMappings);

        if (mapping != null) {
            handleControllerMethod(request, response, mapping, path);
        } else {
            ResponseHandler.send404Response(request, response, path, httpMethod, urlMappings);
        }
    }
    
    private void handleControllerMethod(HttpServletRequest request, HttpServletResponse response, Mapping mapping,
                                       String requestedUrl) throws IOException, ServletException {
        try {
            if (mapping.hasDynamicParams()) {
                List<String> paramValues = mapping.extractParamValues(requestedUrl);
                if (!paramValues.isEmpty()) {
                    System.out.println("   📌 Valeurs extraites: " + paramValues);
                }
            }

            Class<?> clazz = Class.forName(mapping.getClassName());
            Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
            Method method = mapping.getMethod();
            Object[] methodArgs = mapping.prepareMethodArguments(request, requestedUrl);
            Object result = method.invoke(controllerInstance, methodArgs);

            if (result == null) {
                ResponseHandler.sendSimpleResponse(response, "La méthode a retourné NULL");
            } else if (result instanceof String) {
                ResponseHandler.sendSimpleResponse(response, (String) result);
            } else if (result instanceof ModelView) {
                ResponseHandler.forwardToView(request, response, (ModelView) result);
            } else {
                 ResponseHandler.sendSimpleResponse(response, result.toString());
            }
            
        } catch(Exception e) {
            e.printStackTrace();
            ResponseHandler.sendErrorResponse(response, e, mapping);
        }
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
package mg.etu3273.framework;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class StaticResourceFilter implements Filter {
    
    private FilterConfig filterConfig;
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        
        // Vérifier si la ressource existe physiquement
        boolean resourceExists = filterConfig.getServletContext().getResource(path) != null;
        
        if (resourceExists) {
            // La ressource existe, laisser Tomcat la traiter
            chain.doFilter(request, response);
        } else {
            // La ressource n'existe pas, forward vers FrontServlet
            RequestDispatcher dispatcher = httpRequest.getRequestDispatcher("/front-controller");
            dispatcher.forward(request, response);
        }
    }
    
    @Override
    public void destroy() {
        // Nettoyage si nécessaire
    }
}
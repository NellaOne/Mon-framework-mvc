package mg.etu3273.framework;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
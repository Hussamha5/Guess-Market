package server;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

/**
 * Creates the one and only MarketService when the web application starts (nothing is saved to disk:
 * when the server stops, all the data is gone), and frees the user name when a session ends.
 */
@WebListener
public class AppListener implements ServletContextListener, HttpSessionListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        event.getServletContext().setAttribute(MarketService.CONTEXT_ATTRIBUTE, new MarketService());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        ServletContext context = session.getServletContext();
        MarketService service = (MarketService) context.getAttribute(MarketService.CONTEXT_ATTRIBUTE);
        if (service != null) {
            service.logout(session.getId());
        }
    }
}
package server;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/** POST /api/logout - ends the session and frees the user name. The account stays in the market. */
@WebServlet("/api/logout")
public class LogoutServlet extends ApiServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        guarded(response, () -> {
            HttpSession session = request.getSession(false);
            if (session != null) {
                service().logout(session.getId());
                session.invalidate();
            }
            sendOk(response, "You are logged out.");
        });
    }
}
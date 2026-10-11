package server;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/** POST /api/login (name) - logs in with a user name only. Answers with the user name. */
@WebServlet("/api/login")
public class LoginServlet extends ApiServlet {

    /** A client that disappears without logging out frees its user name after this many seconds. */
    private static final int SESSION_TIMEOUT_SECONDS = 60;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        guarded(response, () -> {
            HttpSession session = request.getSession(true);
            service().logout(session.getId());
            String userName = service().login(request.getParameter("name"), session.getId());
            session.setAttribute(AuthFilter.SESSION_USER, userName);
            session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);
            sendOk(response, userName);
        });
    }
}
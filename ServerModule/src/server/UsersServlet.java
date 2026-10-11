package server;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GET /api/users?since=V - name, balance and "is Market Maker" of every user (JSON array of UserDTO). */
@WebServlet("/api/users")
public class UsersServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        guarded(response, () -> {
            long since = optionalLong(request, "since", NO_VERSION);
            sendVersioned(response, service().users(since));
        });
    }
}
package server;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** POST /api/event/close (event, option) - the Market Maker closes an event and declares the winning option (1-based). */
@WebServlet("/api/event/close")
public class CloseServlet extends ApiServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        guarded(response, () -> {
            String event = requiredText(request, "event");
            int option = requiredInt(request, "option", "The winning option number");
            sendOk(response, service().close(currentUser(request), event, option));
        });
    }
}
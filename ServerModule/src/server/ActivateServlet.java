package server;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** POST /api/event/activate (event) - the Market Maker opens an event for trading. */
@WebServlet("/api/event/activate")
public class ActivateServlet extends ApiServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        guarded(response, () -> {
            String event = requiredText(request, "event");
            sendOk(response, service().activate(currentUser(request), event));
        });
    }
}
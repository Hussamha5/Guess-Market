package server;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /api/events?since=V - all the events (every type, every state) as a JSON array of EventDTO.
 * If V is the current version of the market the answer is "204 No Content".
 */
@WebServlet("/api/events")
public class EventsServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        guarded(response, () -> {
            long since = optionalLong(request, "since", NO_VERSION);
            sendVersioned(response, service().events(since));
        });
    }
}
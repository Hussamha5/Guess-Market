package server;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /api/account?since=V&amp;ledgerSince=N - the private data of the logged in user (AccountDTO):
 * balance, his events, and only the account rows after row N (delta fetching).
 */
@WebServlet("/api/account")
public class AccountServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        guarded(response, () -> {
            long since = optionalLong(request, "since", NO_VERSION);
            int ledgerSince = (int) optionalLong(request, "ledgerSince", 0);
            sendVersioned(response, service().account(currentUser(request), since, ledgerSince));
        });
    }
}
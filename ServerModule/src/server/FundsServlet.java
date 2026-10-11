package server;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** POST /api/funds (amount) - loads money to the account of the logged in user. */
@WebServlet("/api/funds")
public class FundsServlet extends ApiServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        guarded(response, () -> {
            double amount = requiredDouble(request, "amount", "The amount");
            sendOk(response, service().loadFunds(currentUser(request), amount));
        });
    }
}
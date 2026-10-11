package server;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * POST /api/event/trade (event, option, side, quantity, price) - buys or sells shares.
 * option is 1-based, side is "buy" or "sell", price is needed only for Order Book events.
 */
@WebServlet("/api/event/trade")
public class TradeServlet extends ApiServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        guarded(response, () -> {
            String event = requiredText(request, "event");
            int option = requiredInt(request, "option", "The option number");
            boolean buy = isBuy(requiredText(request, "side"));
            int quantity = requiredInt(request, "quantity", "The quantity");
            Double price = optionalDouble(request, "price", "The price");
            sendOk(response, service().trade(currentUser(request), event, option, buy, price, quantity));
        });
    }

    private static boolean isBuy(String side) {
        if ("buy".equalsIgnoreCase(side)) {
            return true;
        }
        if ("sell".equalsIgnoreCase(side)) {
            return false;
        }
        throw new IllegalArgumentException("The side must be 'buy' or 'sell' but it is '" + side + "'.");
    }
}
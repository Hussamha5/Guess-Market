package engine;

import java.util.List;
public interface Engine {
    void loadXml(String filePath) throws Exception;

    List<dto.EventDTO> getEvents();

    dto.EventDTO getEventByIndex(int index);

    double[] buyShares(int eventIndex,int optionIndex,int quantity);
    void placeOrder(int eventIndex, String username, int optionIndex, double price, int quantity, boolean isBuy);

    void resolveEvent(int eventIndex,int winIndex);
    List<dto.UserDTO> getUsers();
    void activateEvent(int eventIndex, String username);

}

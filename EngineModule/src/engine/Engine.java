package engine;

import dto.AccountDTO;
import dto.EventDTO;
import dto.UserDTO;

import java.io.InputStream;
import java.util.List;


public interface Engine {

    String getOrCreateUser(String name);

    void loadFunds(String userName, double amount);

    List<String> uploadEvents(String userName, String fileName, InputStream xmlContent);

    String activateEvent(String userName, String eventName);

    String closeEvent(String userName, String eventName, int option);

    String trade(String userName, String eventName, int option, boolean buy, Double price, int quantity);

    List<EventDTO> getEvents();

    List<UserDTO> getUsers();

    AccountDTO getAccount(String userName, int lastSerialSeen);

    long getVersion();
}
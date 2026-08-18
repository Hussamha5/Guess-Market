package engine;

import java.util.List;
public interface Engine {
    void loadXml(String filePath) throws Exception;

    List<dto.EventDTO> getEvents();

    dto.EventDTO getEventByIndex(int index);

    double[] buyShares(int eventIndex,int optionIndex,int quantity);

    void resolveEvent(int eventIndex,int winIndex);
}

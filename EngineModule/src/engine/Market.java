package engine;

import java.util.ArrayList;
import java.util.List;
public class Market implements Engine, java.io.Serializable {
    private List <Event> events;
    public Market()
    {
        this.events = new ArrayList<>();
    }

    @Override
    public java.util.List<dto.EventDTO> getEvents() {
        java.util.List<dto.EventDTO> dtoList = new java.util.ArrayList<>();
        // עוברים על כל האירועים הפנימיים, ממירים אותם ל-DTO, ואוספים לרשימה חדשה
        for (Event e : events) {
            dtoList.add(e.toDTO());
        }
        return dtoList;
    }

    public void addEvent(Event ev)
    {
        this.events.add(ev);
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public Event getByIndex(int index)
    {
        if(index >= 0 && index < events.size())
            return events.get(index);
        return null;
    }
    @Override
    public void loadXml(String filePath) throws Exception {
        List<Event> loadedEvents = XmlParser.loadEvents(filePath);
        this.events = loadedEvents;
    }

    @Override
    public double[] buyShares(int eventIndex, int optionIndex, int quantity) {
        // ניגשים ישירות לרשימת האירועים האמיתית הפנימית של המנוע
        if (eventIndex >= 0 && eventIndex < events.size()) {
            Event event = events.get(eventIndex);
            return event.buyShares(optionIndex, quantity);
        }
        return new double[]{0.0, 0.0, 0.0};
    }

    @Override
    public void resolveEvent(int eventIndex, int winningIndex) {
        // ניגשים ישירות לרשימת האירועים האמיתית הפנימית של המנוע
        if (eventIndex >= 0 && eventIndex < events.size()) {
            Event event = events.get(eventIndex);
            event.resolveEvent(winningIndex);
        }
    }
    @Override
    public dto.EventDTO getEventByIndex(int index) {
        if (index >= 0 && index < events.size()) {
            // שולפים את האירוע וממירים אותו מיד ל-DTO
            return events.get(index).toDTO();
        }
        return null;
    }
}

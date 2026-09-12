package engine;

import java.util.ArrayList;
import java.util.List;
public class Market implements Engine, java.io.Serializable {
    private List <Event> events;
    private List<User> users;
    public Market()
    {
        this.events = new ArrayList<>();
    }

    @Override
    public List<dto.EventDTO> getEvents() {
        List<dto.EventDTO> eventDTOs = new ArrayList<>();
        if (this.events != null) {
            for (Event e : this.events) {
                // במקום לנסות לבנות DTO חלקי, פשוט קוראים לפונקציה המוכנה!
                eventDTOs.add(e.toDTO());
            }
        }
        return eventDTOs;
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
        XmlParser.ParsedData parsedData = XmlParser.loadMarketData(filePath);

        this.events = parsedData.events;
        this.users = parsedData.users;

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
        if (eventIndex >= 0 && eventIndex < events.size()) {
            events.get(eventIndex).resolveEvent(winningIndex, this.users);
        }
    }

    @Override
    public void activateEvent(int eventIndex, String username) {
        if (eventIndex >= 0 && eventIndex < events.size()) {
            Event event = events.get(eventIndex);
            for (User u : this.users) {
                if (u.getName().equals(username)) {
                    event.activateEvent(u);
                    break;
                }
            }
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
    @Override
    public List<dto.UserDTO> getUsers() {
        List<dto.UserDTO> userDTOs = new ArrayList<>();
        if (this.users != null) {
            for (User u : this.users) {
                userDTOs.add(new dto.UserDTO(
                        u.getName(), u.getBalance(), u.getPortfolio(), u.getMmEventIds(),
                        u.getCommissionsPaid() == null ? new java.util.HashMap<>() : u.getCommissionsPaid(),
                        u.getNetInvestments() == null ? new java.util.HashMap<>() : u.getNetInvestments(),
                        u.getFinalPayouts() == null ? new java.util.HashMap<>() : u.getFinalPayouts()
                ));
            }
        }
        return userDTOs;
    }

    @Override
    public void placeOrder(int eventIndex, String username, int optionIndex, double price, int quantity, boolean isBuy) {
        if (eventIndex >= 0 && eventIndex < events.size()) {
            Event event = events.get(eventIndex);
            User currentUser = null;

            // חיפוש המשתמש שיזם את הפעולה
            if (this.users != null) {
                for (User u : this.users) {
                    if (u.getName().equals(username)) {
                        currentUser = u;
                        break;
                    }
                }
            }

            if (currentUser != null) {
                event.placeOrder(currentUser, optionIndex, price, quantity, isBuy,this.users);
            }
        }
    }
    public void processLmsrBuy(int eventIndex, String username, int optionIndex, int quantity) {
        if (eventIndex >= 0 && eventIndex < events.size()) {
            Event event = events.get(eventIndex);
            User buyer = null;
            if (this.users != null) {
                for (User u : this.users) {
                    if (u.getName().equals(username)) {
                        buyer = u; break;
                    }
                }
            }
            if (buyer != null) {
                double[] result = event.buyShares(optionIndex, quantity);

                buyer.updateBalance(-result[2]); // מוריד לו את סך התשלום מהיתרה
                buyer.addShares(event.getId(), optionIndex, quantity);
                buyer.addInvestment(event.getId(), result[0]); // הוספת הוצאה להשקעות
                buyer.addCommission(event.getId(), result[1]); // הוספת עמלה
            }
        }
    }

}

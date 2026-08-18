package ui;

import dto.EventDTO;
import dto.OutcomeDTO;
import dto.TransactionDTO;
import engine.Engine;
import engine.Market;
import engine.BackupManager;
import java.util.Scanner;

public class Main {
    public static void main(String[] args)
    {
        // הממשק (Interface) מקושר למנוע האמיתי
        Engine engine = new Market();
        Scanner sc = new Scanner(System.in);
        boolean running = true;

        System.out.println("Welcome to Hussam's Guess Market");

        while(running)
        {
            printMenu();
            System.out.println("Choose between 1-8 (include)");

            String input = sc.nextLine();
            int currentChoice = -1;
            try {
                currentChoice = Integer.parseInt(input);
            }
            catch (NumberFormatException e){}

            switch (currentChoice)
            {
                case 1 :
                    System.out.println("Please Write the file Path");
                    String path = sc.nextLine();
                    try {
                        engine.loadXml(path);
                        System.out.println("The file was successfully loaded! The system has been reset and updated with the new data.\n");
                    } catch (Exception e){
                        System.out.println("Error in loading the file: " + e.getMessage());
                        System.out.println("previous info are safe(if been)");
                    }
                    break;

                case 2 :
                    // שימוש ב-DTO
                    java.util.List<EventDTO> events = engine.getEvents();

                    if (events.isEmpty()) {
                        System.out.println("-> No events currently in the system. Please load an XML file first (Option 1).");
                    } else {
                        System.out.println("==========================================");
                        System.out.println("               EVENTS LIST                ");
                        System.out.println("==========================================");

                        for (EventDTO e : events) {
                            System.out.println("Event ID    : " + e.getId());
                            System.out.println("Name        : " + e.getName());
                            System.out.println("Description : " + e.getDescription());
                            System.out.println("Commission  : " + e.getCommission() + "%");
                            System.out.println("Comm. Type  : " + e.getCommissionType());

                            OutcomeDTO[] outcomes = e.getOutcomes();
                            System.out.println("Options     : 1) " + outcomes[0].getName() + " | 2) " + outcomes[1].getName());

                            System.out.println("Status      : " + (e.isActive() ? "ACTIVE" : "CLOSED"));
                            System.out.println("------------------------------------------");
                        }
                    }
                    break;

                case 3:
                    java.util.List<EventDTO> currentEvents = engine.getEvents();
                    if (currentEvents.isEmpty()) {
                        System.out.println("-> No events currently in the system. Please load an XML file first.");
                        break;
                    }

                    System.out.println("--- Select an Event ---");
                    for (int i = 0; i < currentEvents.size(); i++) {
                        System.out.println((i + 1) + ". " + currentEvents.get(i).getName());
                    }
                    System.out.print("Enter event number: ");

                    String eventInput = sc.nextLine();
                    int eventIndex = -1;
                    try {
                        eventIndex = Integer.parseInt(eventInput) - 1;
                    } catch (NumberFormatException e) {}

                    EventDTO selectedEvent = engine.getEventByIndex(eventIndex);
                    if (selectedEvent == null) {
                        System.out.println("-> Error: Invalid event selection.");
                        break;
                    }

                    printEventState(selectedEvent);
                    break;

                case 4:
                    java.util.List<EventDTO> allEvents = engine.getEvents();
                    // שמירת האינדקסים המקוריים של האירועים הפעילים
                    java.util.List<Integer> activeIndices = new java.util.ArrayList<>();

                    for (int i = 0; i < allEvents.size(); i++) {
                        if (allEvents.get(i).isActive()) {
                            activeIndices.add(i);
                        }
                    }

                    if (activeIndices.isEmpty()) {
                        System.out.println("-> No active events available for trading.");
                        break;
                    }

                    System.out.println("--- Select an Active Event ---");
                    for (int i = 0; i < activeIndices.size(); i++) {
                        System.out.println((i + 1) + ". " + allEvents.get(activeIndices.get(i)).getName());
                    }
                    System.out.print("Enter event number: ");

                    String evtInput = sc.nextLine();
                    int activeIndex = -1;
                    try {
                        activeIndex = Integer.parseInt(evtInput) - 1;
                    } catch (NumberFormatException e) {}

                    if (activeIndex < 0 || activeIndex >= activeIndices.size()) {
                        System.out.println("-> Error: Invalid event selection.");
                        break;
                    }

                    // שליפת האינדקס המקורי במנוע כדי למנוע קריסה
                    int originalEventIndex = activeIndices.get(activeIndex);
                    EventDTO selectedEvt = engine.getEventByIndex(originalEventIndex);

                    printEventState(selectedEvt);

                    System.out.println("Select option to buy shares for:");
                    OutcomeDTO[] outs = selectedEvt.getOutcomes();
                    System.out.println("1. " + outs[0].getName());
                    System.out.println("2. " + outs[1].getName());
                    System.out.print("Enter option number (1 or 2): ");

                    String optInput = sc.nextLine();
                    int optIndex = -1;
                    try {
                        optIndex = Integer.parseInt(optInput) - 1;
                    } catch (NumberFormatException e) {}

                    if (optIndex != 0 && optIndex != 1) {
                        System.out.println("-> Error: Invalid option selection.");
                        break;
                    }

                    System.out.print("Enter amount of shares to buy: ");
                    String qtyInput = sc.nextLine();
                    int quantity = -1;
                    try {
                        quantity = Integer.parseInt(qtyInput);
                    } catch (NumberFormatException e) {}

                    if (quantity <= 0) {
                        System.out.println("-> Error: Quantity must be a positive number.");
                        break;
                    }

                    // שימוש במנוע דרך ה-Interface!
                    double[] result = engine.buyShares(originalEventIndex, optIndex, quantity);

                    System.out.println();
                    System.out.println("==================================================");
                    System.out.println("               PURCHASE SUCCESSFUL!               ");
                    System.out.println("==================================================");
                    System.out.printf("Shares Cost : $%.2f\n", result[0]);
                    if (result[1] > 0) {
                        System.out.printf("Commission  : $%.2f\n", result[1]);
                    }
                    System.out.printf("Total Paid  : $%.2f\n", result[2]);
                    System.out.println("==================================================");
                    System.out.println();

                    // משיכת האירוע מחדש כדי להדפיס את הנתונים המעודכנים
                    selectedEvt = engine.getEventByIndex(originalEventIndex);
                    printEventState(selectedEvt);
                    break;

                case 5:
                    java.util.List<EventDTO> allEvts = engine.getEvents();
                    java.util.List<Integer> activeCloseIndices = new java.util.ArrayList<>();

                    for (int i = 0; i < allEvts.size(); i++) {
                        if (allEvts.get(i).isActive()) {
                            activeCloseIndices.add(i);
                        }
                    }

                    if (activeCloseIndices.isEmpty()) {
                        System.out.println("-> No active events available to close.");
                        break;
                    }

                    System.out.println("--- Select an Active Event to Close ---");
                    for (int i = 0; i < activeCloseIndices.size(); i++) {
                        System.out.println((i + 1) + ". " + allEvts.get(activeCloseIndices.get(i)).getName());
                    }
                    System.out.print("Enter event number: ");

                    String closeEvtInput = sc.nextLine();
                    int closeIndex = -1;
                    try {
                        closeIndex = Integer.parseInt(closeEvtInput) - 1;
                    } catch (NumberFormatException e) {}

                    if (closeIndex < 0 || closeIndex >= activeCloseIndices.size()) {
                        System.out.println("-> Error: Invalid event selection.");
                        break;
                    }

                    int originalCloseIndex = activeCloseIndices.get(closeIndex);
                    EventDTO eventToClose = engine.getEventByIndex(originalCloseIndex);

                    System.out.println("Select the WINNING option:");
                    OutcomeDTO[] eventOuts = eventToClose.getOutcomes();
                    System.out.println("1. " + eventOuts[0].getName());
                    System.out.println("2. " + eventOuts[1].getName());
                    System.out.print("Enter winning option number (1 or 2): ");

                    String winInput = sc.nextLine();
                    int winIndex = -1;
                    try {
                        winIndex = Integer.parseInt(winInput) - 1;
                    } catch (NumberFormatException e) {}

                    if (winIndex != 0 && winIndex != 1) {
                        System.out.println("-> Error: Invalid option selection.");
                        break;
                    }

                    // סגירת האירוע דרך המנוע
                    engine.resolveEvent(originalCloseIndex, winIndex);

                    System.out.println();
                    System.out.println("==================================================");
                    System.out.println("         EVENT CLOSED SUCCESSFULLY!               ");
                    System.out.println(" Winning Option: " + eventOuts[winIndex].getName());
                    System.out.println("==================================================");
                    System.out.println();

                    // הצגת המצב הסופי לאחר הסגירה
                    eventToClose = engine.getEventByIndex(originalCloseIndex);
                    printEventState(eventToClose);
                    break;

                case 6:
                    System.out.print("Enter full path to save the system (without extension): ");
                    String savePath = sc.nextLine();
                    try {
                        BackupManager.saveMarket((Market) engine, savePath);
                        System.out.println("-> System state saved successfully!");
                    } catch (Exception e) {
                        System.out.println("-> Error saving system: " + e.getMessage());
                    }
                    break;

                case 7:
                    System.out.print("Enter full path to load the system (without extension): ");
                    String loadPath = sc.nextLine();
                    try {
                        engine.Market loadedMarket = BackupManager.loadMarket(loadPath);
                        engine = loadedMarket;
                        System.out.println("-> System state loaded successfully!");
                    } catch (Exception e) {
                        System.out.println("-> Error loading system: " + e.getMessage());
                    }
                    break;

                case 8 :
                    System.out.println("Closing System... BYE BYE!!");
                    running = false;
                    break;

                default:
                    System.out.println("Wrong input !!!");
            }
            System.out.println();
        }
        sc.close();
    }

    private static void printMenu() {
        System.out.println("--- Main Menu ---");
        System.out.println("1. Read the System files (XML)");
        System.out.println("2. Show events");
        System.out.println("3. Event state");
        System.out.println("4. Participation in the event (purchasing shares)");
        System.out.println("5. Close Event");
        System.out.println("6. Save System State");
        System.out.println("7. Load System State");
        System.out.println("8. Exit System");
    }

    // הפעולה מקבלת אך ורק EventDTO, ללא הלוגיקה החשופה של המנוע האמיתי
    private static void printEventState(EventDTO selectedEvent) {
        System.out.println("==================================================");
        System.out.println("               EVENT DETAILS (ID: " + selectedEvent.getId() + ")");
        System.out.println("==================================================");
        System.out.println("Name        : " + selectedEvent.getName());
        System.out.println("Description : " + selectedEvent.getDescription());
        System.out.println("Status      : " + (selectedEvent.isActive() ? "ACTIVE" : "CLOSED"));
        System.out.println("Commission  : " + selectedEvent.getCommission() + "% (" + selectedEvent.getCommissionType() + ")");

        System.out.printf("Acc. Balance: $%.2f\n", selectedEvent.getMmBalance());
        System.out.printf("Total Fees  : $%.2f\n", selectedEvent.getTotalFees());
        System.out.println();

        System.out.println("--- Current Option Pricing & Distribution ---");
        System.out.println("--------------------------------------------------");
        System.out.printf("%-20s | %-20s | %s\n", "Option", "Price (Prob %)", "Shares Bought");
        System.out.println("--------------------------------------------------");

        OutcomeDTO[] outs = selectedEvent.getOutcomes();
        for (int i = 0; i < outs.length; i++) {
            double price = outs[i].getPrice();
            double probPct = price * 100.0;
            String priceStr = String.format("$%.2f (%.1f%%)", price, probPct);
            System.out.printf("%-20s | %-20s | %d\n", outs[i].getName(), priceStr, outs[i].getQuantity());
        }
        System.out.println("--------------------------------------------------");
        System.out.println();

        System.out.println("--- Trade History Audit Log ---");
        System.out.println("--------------------------------------------------");
        System.out.printf("%-20s | %-20s | %s\n", "Option Selected", "Quantity", "Total Paid");
        System.out.println("--------------------------------------------------");

        java.util.List<TransactionDTO> transactions = selectedEvent.getTransactions();
        if (transactions.isEmpty()) {
            System.out.println("No trades yet.");
        } else {
            for (TransactionDTO t : transactions) {
                // עודכן לקרוא לשמות הפונקציות הנכונות של ה-TransactionDTO שיצרנו
                System.out.printf("%-20s | %-20s | $%.2f\n", t.getOutcomeName(), t.getQuantity(), t.getTotalPaid());
            }
        }
        System.out.println("==================================================");
    }
}
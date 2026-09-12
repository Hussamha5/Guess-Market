package engine;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

public class XmlParser {

    public static class ParsedData {
        public List<Event> events;
        public List<User> users;

        public ParsedData(List<Event> events, List<User> users) {
            this.events = events;
            this.users = users;
        }
    }

    public static ParsedData loadMarketData(String filePath) throws Exception {
        if (!filePath.toLowerCase().endsWith(".xml")) {
            throw new Exception("File type needs to be XML (ends with .xml).");
        }

        File xmlFile = new File(filePath);
        if (!xmlFile.exists()) {
            throw new Exception("File does not exist in the given path.");
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);
        document.getDocumentElement().normalize();

        List<Event> newEvents = new ArrayList<>();
        List<User> newUsers = new ArrayList<>();

        Set<String> uniqueEventIds = new HashSet<>();
        Set<String> uniqueUserNames = new HashSet<>();
        Map<String, Integer> eventMmCount = new HashMap<>();

        // ----------------- Step A: Parsing Events -----------------
        NodeList eventList = document.getElementsByTagName("GM-event");
        for (int i = 0; i < eventList.getLength(); i++) {
            Node node = eventList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element eventElement = (Element) node;

                String name = eventElement.getAttribute("name");
                String id = eventElement.getElementsByTagName("id").item(0).getTextContent().trim();
                String description = eventElement.getElementsByTagName("description").item(0).getTextContent().trim();

                NodeList commList = eventElement.getElementsByTagName("commission");
                if (commList.getLength() == 0) commList = eventElement.getElementsByTagName("comision");
                Element commissionElement = (Element) commList.item(0);

                String commissionStr = commissionElement.getTextContent().trim();
                String commissionType = commissionElement.getAttribute("type");

                int commission;
                try {
                    commission = Integer.parseInt(commissionStr);
                } catch (NumberFormatException e) {
                    throw new Exception("The commission for the event '" + name + "' is not a valid number.");
                }

                if (commission < 0 || commission > 90) {
                    throw new Exception("Illegal commission on event '" + name + "'. The commission must be between 0 and 90.");
                }

                if (uniqueEventIds.contains(id)) {
                    throw new Exception("Error! The Event ID '" + id + "' appears more than once in the file.");
                }
                uniqueEventIds.add(id);
                eventMmCount.put(id, 0);

                NodeList optionNodes = eventElement.getElementsByTagName("GM-option");
                if (optionNodes.getLength() != 2) {
                    throw new Exception("Event error '" + name + "': There must be exactly two options.");
                }

                String[] outcomeNames = new String[2];
                outcomeNames[0] = optionNodes.item(0).getTextContent().trim();
                outcomeNames[1] = optionNodes.item(1).getTextContent().trim();

                Element methodElement = (Element) eventElement.getElementsByTagName("GM-method").item(0);

                boolean isLMSR = methodElement.getElementsByTagName("GM-LMSR").getLength() > 0;
                boolean isOrderBook = methodElement.getElementsByTagName("GM-order-book").getLength() > 0;

                Event newEvent = null;

                if (isLMSR) {
                    Element lmsrElement = (Element) methodElement.getElementsByTagName("GM-LMSR").item(0);
                    String bStr = lmsrElement.getElementsByTagName("b").item(0).getTextContent().trim();
                    int b;
                    try {
                        b = Integer.parseInt(bStr);
                    } catch (NumberFormatException e) {
                        throw new Exception("Error! For event '" + name + "', the 'b' parameter is not a valid number.");
                    }
                    newEvent = new Event(id, name, description, commission, commissionType, outcomeNames, b, 0, 0, false);
                } else if (isOrderBook) {
                    Element obElement = (Element) methodElement.getElementsByTagName("GM-order-book").item(0);
                    int d = Integer.parseInt(obElement.getAttribute("d"));

                    String initialStr = obElement.getAttribute("inital");
                    if (initialStr == null || initialStr.isEmpty()) {
                        initialStr = obElement.getAttribute("initial");
                    }
                    int initial = Integer.parseInt(initialStr);
                    boolean allowMint = Boolean.parseBoolean(obElement.getAttribute("allow-mint"));

                    newEvent = new Event(id, name, description, commission, commissionType, outcomeNames, 0, d, initial, allowMint);
                }

                if (newEvent != null) {
                    newEvents.add(newEvent);
                }
            }
        }

        NodeList userList = document.getElementsByTagName("GM-user");
        for (int i = 0; i < userList.getLength(); i++) {
            Node node = userList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element userElement = (Element) node;

                String userName = userElement.getAttribute("name").trim();

                if (uniqueUserNames.contains(userName)) {
                    throw new Exception("Error! The username '" + userName + "' appears more than once.");
                }
                uniqueUserNames.add(userName);

                String initialCashStr = userElement.getElementsByTagName("initial-cash").item(0).getTextContent().trim();
                double initialCash;
                try {
                    initialCash = Double.parseDouble(initialCashStr);
                } catch (NumberFormatException e) {
                    throw new Exception("Error! Invalid initial cash for user '" + userName + "'.");
                }

                if (initialCash <= 0) {
                    throw new Exception("Error! Initial cash for user '" + userName + "' must be greater than 0 (entered: " + initialCash + ").");
                }

                User newUser = new User(userName, initialCash);

                NodeList mmList = userElement.getElementsByTagName("GM-mareket-maker");
                if (mmList.getLength() == 0) {
                    mmList = userElement.getElementsByTagName("GM-market-maker");
                }
                if (mmList.getLength() > 0) {
                    Element mmElement = (Element) mmList.item(0);
                    NodeList mmEvents = mmElement.getElementsByTagName("event");
                    for (int j = 0; j < mmEvents.getLength(); j++) {
                        Element evEl = (Element) mmEvents.item(j);
                        String mmEventId = evEl.getAttribute("id").trim();

                        if (!uniqueEventIds.contains(mmEventId)) {
                            throw new Exception("Error! User '" + userName + "' is defined as MM for event '" + mmEventId + "' which does not exist in the file.");
                        }

                        newUser.addMmEventId(mmEventId);
                        eventMmCount.put(mmEventId, eventMmCount.get(mmEventId) + 1);
                    }
                }

                newUsers.add(newUser);
            }
        }

        for (Map.Entry<String, Integer> entry : eventMmCount.entrySet()) {
            if (entry.getValue() != 1) {
                throw new Exception("Error! Event ID '" + entry.getKey() + "' has " + entry.getValue() + " Market Makers (MM). Exactly one must be defined.");
            }
        }

        return new ParsedData(newEvents, newUsers);
    }
}
package engine;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class XmlParser {


    public static List<Event> loadEvents(String filePath) throws Exception {


        if (!filePath.toLowerCase().endsWith(".xml")) {
            throw new Exception("File type needs to be xml (ends with .xml)");
        }

        File xmlFile = new File(filePath);


        if (!xmlFile.exists()) {
            throw new Exception("file does not exist in path");
        }


        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);
        document.getDocumentElement().normalize();

        List<Event> newEvents = new ArrayList<>();
        Set<String> uniqueIds = new HashSet<>();


        NodeList eventList = document.getElementsByTagName("GM-event");

        for (int i = 0; i < eventList.getLength(); i++) {
            Node node = eventList.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element eventElement = (Element) node;


                String name = eventElement.getAttribute("name");
                String id = eventElement.getElementsByTagName("id").item(0).getTextContent().trim();
                String description = eventElement.getElementsByTagName("description").item(0).getTextContent().trim();
                Element commissionElement = (Element) eventElement.getElementsByTagName("comision").item(0);
                String commissionStr = commissionElement.getTextContent().trim();
                String commissionType = commissionElement.getAttribute("type");


                // בדיקת עמלה
                int commission;
                try {
                    commission = Integer.parseInt(commissionStr);
                } catch (NumberFormatException e) {
                    throw new Exception("The commission for the event'" + name + "' it is not a valid number.");
                }

                if (commission < 0 || commission > 90) {
                    throw new Exception("Illegal commission on an event '" + name + "'. The commission must be between 0 and 90.");
                }

                // בדיקת מזהה כפול
                if (uniqueIds.contains(id)) {
                    throw new Exception("Error! The ID '" + id + "' appears more than once in the file.");
                }
                uniqueIds.add(id);

                // קריאת שתי האפשרויות (Outcomes)
                NodeList optionNodes = eventElement.getElementsByTagName("GM-option");
                if (optionNodes.getLength() != 2) {
                    throw new Exception("Event error'" + name + "': There must be exactly two options (GM-option).");
                }

                String[] outcomeNames = new String[2];
                outcomeNames[0] = optionNodes.item(0).getTextContent().trim();
                outcomeNames[1] = optionNodes.item(1).getTextContent().trim();


                Element methodElement = (Element) eventElement.getElementsByTagName("GM-method").item(0);
                Element lmsrElement = (Element) methodElement.getElementsByTagName("GM-LMSR").item(0);
                String bStr = lmsrElement.getElementsByTagName("b").item(0).getTextContent().trim();
                int b;
                try {
                    b = Integer.parseInt(bStr);
                } catch (NumberFormatException e) {
                    throw new Exception("ERROR! '" + name + "'The b parameter is not a valid number :");
                }

                // יצירת האירוע עם b והוספתו לרשימה
                Event newEvent = new Event(id, name, description, commission, commissionType, outcomeNames, b);
                newEvents.add(newEvent);
            }
        }


        return newEvents;
    }
}
package engine;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reads an events file (Exercise 3 format) from a stream and checks it.
 * The content is only read into memory - it is never written to the disk of the server.
 * If the file has problems, ALL the problems that were found are reported in one message
 * (an IllegalArgumentException), and nothing is created.
 */
public class XmlParser {

    public static final int MAX_FILE_BYTES = 2 * 1024 * 1024;

    private static final int MIN_OPTIONS = 2;
    private static final int MAX_OPTIONS = 10;
    private static final int MIN_COMMISSION = 0;
    private static final int MAX_COMMISSION = 90;
    private static final int MAX_PROBLEMS_SHOWN = 15;

    private XmlParser() {
    }

    /**
     * @param input         the content of the file
     * @param marketMaker   the user who uploaded the file - he is the Market Maker of all its events
     * @param firstNumber   the number of the first event of the file (the events are numbered from 1)
     * @param existingNames the names (in lower case) of the events that are already in the market
     */
    public static List<Event> parse(InputStream input, User marketMaker, int firstNumber,
                                    Set<String> existingNames) {
        Element root = readRoot(input);
        if (!"Guess-Market".equals(root.getTagName())) {
            throw new IllegalArgumentException("The root element must be <Guess-Market> but the file starts with <"
                    + root.getTagName() + ">.");
        }
        if (firstChild(root, "GM-users") != null) {
            throw new IllegalArgumentException("The file contains <GM-users>. Users are not defined in files any "
                    + "more (Exercise 3 format): every user logs in by name, and the user who uploads the file "
                    + "becomes the Market Maker of its events.");
        }
        Element eventsElement = firstChild(root, "GM-events");
        List<Element> eventElements = eventsElement == null ? new ArrayList<>() : children(eventsElement, "GM-event");
        if (eventElements.isEmpty()) {
            throw new IllegalArgumentException(
                    "The file does not contain any event (<GM-events> with <GM-event> elements).");
        }
        return parseEvents(eventElements, marketMaker, firstNumber, existingNames);
    }

    private static List<Event> parseEvents(List<Element> eventElements, User marketMaker, int firstNumber,
                                           Set<String> existingNames) {
        List<String> problems = new ArrayList<>();
        List<Event> events = new ArrayList<>();
        Set<String> namesInFile = new HashSet<>();
        for (int i = 0; i < eventElements.size(); i++) {
            EventParser parser = new EventParser(i + 1, eventElements.get(i), problems, namesInFile, existingNames);
            Event event = parser.parse(firstNumber + events.size(), marketMaker);
            if (event != null) {
                events.add(event);
            }
        }
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException(describe(problems));
        }
        return events;
    }

    private static String describe(List<String> problems) {
        StringBuilder message = new StringBuilder("The file is not valid (" + problems.size() + " problem(s)):");
        for (int i = 0; i < Math.min(problems.size(), MAX_PROBLEMS_SHOWN); i++) {
            message.append("\n- ").append(problems.get(i));
        }
        if (problems.size() > MAX_PROBLEMS_SHOWN) {
            message.append("\n- ... and ").append(problems.size() - MAX_PROBLEMS_SHOWN).append(" more.");
        }
        return message.toString();
    }

    // ----- reading the XML -----

    private static Element readRoot(InputStream input) {
        byte[] content = readLimited(input);
        if (content.length == 0) {
            throw new IllegalArgumentException("The file is empty.");
        }
        try {
            DocumentBuilder builder = createBuilder();
            Document document = builder.parse(new ByteArrayInputStream(content));
            return document.getDocumentElement();
        } catch (SAXParseException e) {
            throw new IllegalArgumentException("The file is not a well-formed XML document: " + e.getMessage()
                    + " (line " + e.getLineNumber() + ", column " + e.getColumnNumber() + ").");
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new IllegalArgumentException("The file could not be read as an XML document: " + e.getMessage());
        }
    }

    private static byte[] readLimited(InputStream input) {
        try {
            byte[] content = input.readNBytes(MAX_FILE_BYTES + 1);
            if (content.length > MAX_FILE_BYTES) {
                throw new IllegalArgumentException("The file is too big. The maximum size is "
                        + MAX_FILE_BYTES / 1024 + " KB.");
            }
            return content;
        } catch (IOException e) {
            throw new IllegalArgumentException("The file could not be read: " + e.getMessage());
        }
    }

    // a parser that does not read external files or entities (a malicious file cannot read the server's disk)
    private static DocumentBuilder createBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler());
        return builder;
    }

    // ----- small DOM helpers (only direct children are looked at) -----

    private static List<Element> children(Element parent, String tag) {
        List<Element> found = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && tag.equals(((Element) node).getTagName())) {
                found.add((Element) node);
            }
        }
        return found;
    }

    private static Element firstChild(Element parent, String... tags) {
        for (String tag : tags) {
            List<Element> found = children(parent, tag);
            if (!found.isEmpty()) {
                return found.get(0);
            }
        }
        return null;
    }

    /** Checks and converts one {@code <GM-event>} element. Every problem is added to the shared list. */
    private static final class EventParser {

        private final int position;
        private final Element element;
        private final List<String> problems;
        private final Set<String> namesInFile;
        private final Set<String> existingNames;
        private final String name;

        EventParser(int position, Element element, List<String> problems, Set<String> namesInFile,
                    Set<String> existingNames) {
            this.position = position;
            this.element = element;
            this.problems = problems;
            this.namesInFile = namesInFile;
            this.existingNames = existingNames;
            this.name = element.getAttribute("name").trim();
        }

        Event parse(int number, User marketMaker) {
            int problemsBefore = problems.size();
            checkName();
            String description = parseDescription();
            Integer commission = null;
            String commissionType = null;
            Element commissionElement = firstChild(element, "commission", "comision");
            if (commissionElement == null) {
                problem("the <commission> element is missing.");
            } else {
                commissionType = parseCommissionType(commissionElement);
                commission = parseNumber(commissionElement.getTextContent(), "commission",
                        MIN_COMMISSION, MAX_COMMISSION);
            }
            List<String> options = parseOptions();
            Event event = parseMethod(number, marketMaker, description, commission, commissionType, options);
            return problems.size() == problemsBefore ? event : null;
        }

        private void checkName() {
            String key = name.toLowerCase(Locale.ROOT);
            if (name.isEmpty()) {
                problem("the 'name' attribute is missing or empty.");
            } else if (!namesInFile.add(key)) {
                problem("the name appears more than once in the file (names are not case sensitive).");
            } else if (existingNames.contains(key)) {
                problem("an event with this name already exists in the market (event names must be unique, "
                        + "and are not case sensitive).");
            }
        }

        private String parseDescription() {
            Element description = firstChild(element, "description");
            if (description == null) {
                problem("the <description> element is missing.");
                return "";
            }
            return description.getTextContent().trim();
        }

        private String parseCommissionType(Element commissionElement) {
            String type = commissionElement.getAttribute("type").trim().toLowerCase(Locale.ROOT);
            if (!Event.FEE_ON_CLOSE.equals(type) && !Event.FEE_ON_PURCHASE.equals(type)) {
                problem("the commission type must be '" + Event.FEE_ON_CLOSE + "' or '" + Event.FEE_ON_PURCHASE
                        + "' but it is '" + type + "'.");
            }
            return type;
        }

        private List<String> parseOptions() {
            List<String> options = new ArrayList<>();
            Element optionsElement = firstChild(element, "GM-options");
            if (optionsElement == null) {
                problem("the <GM-options> element is missing.");
                return options;
            }
            Set<String> seen = new HashSet<>();
            for (Element option : children(optionsElement, "GM-option")) {
                String text = option.getTextContent().trim();
                if (text.isEmpty()) {
                    problem("an option has no text.");
                } else if (!seen.add(text.toLowerCase(Locale.ROOT))) {
                    problem("the option '" + text + "' appears more than once.");
                }
                options.add(text);
            }
            if (options.size() < MIN_OPTIONS || options.size() > MAX_OPTIONS) {
                problem("an event needs between " + MIN_OPTIONS + " and " + MAX_OPTIONS
                        + " options but it has " + options.size() + ".");
            }
            return options;
        }

        private Event parseMethod(int number, User marketMaker, String description, Integer commission,
                                  String commissionType, List<String> options) {
            Element method = firstChild(element, "GM-method");
            if (method == null) {
                problem("the <GM-method> element is missing.");
                return null;
            }
            Element lmsr = firstChild(method, "GM-LMSR");
            Element orderBook = firstChild(method, "GM-order-book");
            if ((lmsr == null) == (orderBook == null)) {
                problem("<GM-method> must contain exactly one of <GM-LMSR> or <GM-order-book>.");
                return null;
            }
            String[] optionNames = options.toArray(new String[0]);
            if (lmsr != null) {
                Element b = firstChild(lmsr, "b");
                if (b == null) {
                    problem("the <b> element of <GM-LMSR> is missing.");
                    return null;
                }
                Integer liquidity = parseNumber(b.getTextContent(), "b", 1, Integer.MAX_VALUE);
                if (liquidity == null || commission == null) {
                    return null;
                }
                return new Event(number, name, description, commission, commissionType, optionNames,
                        liquidity, 0, 0, false, marketMaker);
            }
            Integer baseValue = parseNumber(orderBook.getAttribute("d"), "d", 1, Integer.MAX_VALUE);
            String initialText = orderBook.hasAttribute("initial")
                    ? orderBook.getAttribute("initial") : orderBook.getAttribute("inital");
            Integer initial = parseNumber(initialText, "initial", 0, Integer.MAX_VALUE);
            Boolean allowMint = parseBoolean(orderBook.getAttribute("allow-mint"));
            if (baseValue == null || initial == null || allowMint == null || commission == null) {
                return null;
            }
            return new Event(number, name, description, commission, commissionType, optionNames,
                    0, baseValue, initial, allowMint, marketMaker);
        }

        private Integer parseNumber(String text, String what, int min, int max) {
            String trimmed = text == null ? "" : text.trim();
            try {
                int value = Integer.parseInt(trimmed);
                if (value < min || value > max) {
                    String range = max == Integer.MAX_VALUE ? "at least " + min : "between " + min + " and " + max;
                    problem("'" + what + "' must be " + range + " but it is " + value + ".");
                    return null;
                }
                return value;
            } catch (NumberFormatException e) {
                problem("'" + what + "' must be a whole number but it is '" + trimmed + "'.");
                return null;
            }
        }

        private Boolean parseBoolean(String text) {
            String trimmed = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
            if ("true".equals(trimmed) || "false".equals(trimmed)) {
                return Boolean.valueOf(trimmed);
            }
            problem("'allow-mint' must be true or false but it is '" + trimmed + "'.");
            return null;
        }

        private void problem(String text) {
            String who = name.isEmpty() ? "Event #" + position : "Event #" + position + " ('" + name + "')";
            problems.add(who + ": " + text);
        }
    }
}
package com.gravitee.migration.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.util.Objects.isNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringUtils {

    /**
     * Removes all newlines from the given string.
     *
     * @param input the input string
     * @return the string without newlines
     */
    public static String removeNewlines(String input) {
        if (isNull(input) || input.isEmpty()) {
            return input;
        }

        return input.replaceAll("\\r?\\n", "");
    }

    /**
     * Converts a Document object to a string representation.
     *
     * @param doc the Document object to convert
     * @return the string representation of the Document
     * @throws Exception if an error occurs during conversion
     */
    public static String convertDocumentToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();

        // Disable access to external DTDs and stylesheets
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

        Transformer transformer = tf.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.getBuffer().toString();
    }

    /**
     * Builds a key fragment string from the given NodeList of key fragments.
     *
     * @param keyFragments the NodeList of key fragments
     * @return the constructed key fragment string
     */
    public static String buildKeyFragmentString(NodeList keyFragments) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < keyFragments.getLength(); i++) {
            Node keyFragment = keyFragments.item(i);

            if (hasRefAttribute(keyFragment)) {
                appendRefValue(result, keyFragment);
                break; // Stop processing further KeyFragments
            } else {
                appendTextContent(result, keyFragment);
            }
        }

        return result.toString();
    }

    /**
     * Adds a prefix to the key if the prefix is not null or empty.
     *
     * @param prefix the prefix to add
     * @param key    the original key
     * @return the key with the prefix added
     */
    public static String addPrefixToKey(String prefix, String key) {
        if (prefix != null && !prefix.isEmpty()) {
            return prefix + "__" + key;
        }
        return key;
    }


    /**
     * Constructs an endpoints URL by replacing the host part with a placeholder.
     *
     * @param url the original URL
     * @return the transformed URL with a placeholder for the host
     */
    public static String constructEndpointsUrl(String url) {
        // Extract the protocol part (e.g., "http://" or "https://")
        String protocol = url.substring(0, url.indexOf("//") + 2);

        // Extract the part between the protocol and the next "/"
        String baseUrl = url.substring(protocol.length());
        String host = baseUrl.split("/")[0]; // Get the host part

        // Check if the host contains a placeholder or is already in lowercase
        if (host.matches(".*\\{.*}.*") || host.equals(host.toLowerCase())) {
            return url; // Return the URL as is
        }

        // Transform the URL
        return protocol + "{#context.attributes['" + host + "']}" + url.substring(protocol.length() + host.length());
    }

    public static String readFileFromClasspath(String filePath) throws Exception {
        var classLoader = StringUtils.class.getClassLoader();
        var resource = classLoader.getResource(filePath);
        if (resource == null) {
            throw new IllegalArgumentException("File not found in classpath: " + filePath);
        }
        return Files.readString(Paths.get(resource.toURI()));
    }

    private static boolean hasRefAttribute(Node keyFragment) {
        return keyFragment.getAttributes() != null && keyFragment.getAttributes().getNamedItem("ref") != null;
    }

    private static void appendRefValue(StringBuilder result, Node keyFragment) {
        String refValue = keyFragment.getAttributes().getNamedItem("ref").getNodeValue();
        result.append(":{#context.attributes['").append(refValue).append("']}");
    }

    private static void appendTextContent(StringBuilder result, Node keyFragment) {
        if (!result.isEmpty()) {
            result.append("__");
        }
        result.append(keyFragment.getTextContent());
    }
}

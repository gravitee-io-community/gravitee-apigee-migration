package com.gravitee.migration.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.stream.Collectors;

import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.PolicyType.JWT;
import static java.util.Objects.isNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringUtils {

    private static final String KEY_FRAGMENT_SEPARATOR = "__";

    /**
     * Removes all newlines from the given string.
     *
     * @param input the input string
     * @return the string without newlines
     */
    public static String removeNewLines(String input) {
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
     * @throws TransformerException if an error occurs during conversion
     */
    public static String convertDocumentToString(Document doc) throws TransformerException {
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
     * Builds a cache key from the given key fragments and prefix.
     *
     * @param cacheKeyFragments the NodeList of cache key fragments
     * @param prefix            the prefix to add to the key
     * @return the constructed cache key
     */
    public static String buildCacheKey(NodeList cacheKeyFragments, String prefix) {
        // Builds a key fragment string from the given key fragments in the <KeyFragment> tag.
        var key = buildKeyFragmentString(cacheKeyFragments);
        return addPrefixToKey(prefix, key);
    }

    /**
     * Builds a key fragment string from the given key fragments in the <KeyFragment> tag.
     * Concatenates the key fragments with "__" and handles the "ref" attribute.
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
            return prefix + KEY_FRAGMENT_SEPARATOR + key;
        }
        return key;
    }

    /**
     * Checks if a string is not null and not empty.
     *
     * @param str the string to check
     * @return true if the string is not null and not empty, false otherwise
     */
    public static boolean isNotNullOrEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    /**
     * Constructs an endpoints URL by replacing the host part with a placeholder.
     *
     * @param url the original URL
     * @return the transformed URL with a placeholder for the host
     */
    public static String constructEndpointsUrl(String url, String dictionaryName) {
        // Extract the protocol part (e.g., "http://" or "https://")
        String protocol = url.substring(0, url.indexOf("//") + 2);

        // Extract the part between the protocol and the next "/"
        String baseUrl = url.substring(protocol.length());
        String host = baseUrl.split("/")[0]; // Get the host part (e.g., "example.com")

        // Check if the host contains a placeholder or is already in lowercase (e.g., DYNAMIC_URL or example.com")
        if (host.matches(".*\\{.*}.*") || host.equals(host.toLowerCase())) {
            return url; // Return the URL as is
        }

        // Transform the URL
        return protocol + String.format(DICTIONARY_FORMAT_WRAPPED, dictionaryName, host) + url.substring(protocol.length() + host.length());
    }

    /**
     * Removes curly braces from the input string.
     *
     * @param input the input string
     * @return the string without curly braces
     */
    public static String removeCurlyBraces(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.replace("{", "").replace("}", "");
    }

    /**
     * Extracts the base URL from the given URL.
     *
     * @param url the original URL
     * @return the base URL (e.g., "example.com", "DYNAMIC_URL")
     */
    public static String extractBaseUrl(String url) {
        // Extract the protocol part (e.g., "http://" or "https://")
        String protocol = url.substring(0, url.indexOf("//") + 2);

        // Extract the part between the protocol and the next "/"
        String baseUrl = url.substring(protocol.length());

        return baseUrl.split("/")[0];
    }

    /**
     * Wraps a value in context attributes format.
     *
     * @param value the value to wrap
     * @return the wrapped value (e.g., "{#context.attributes['value']}")
     */
    public static String wrapValueInContextAttributes(String value) {
        return String.format(CONTEXT_ATTRIBUTE_FORMAT_WRAPPED, value);
    }

    /**
     * Reads a Groovy policy file from the specified absolute path.
     *
     * @return the content of the Groovy policy file as a string
     * @throws IOException if an error occurs while reading the file
     */
    public static String readGroovyPolicy(String resourcePath) throws IOException {
        InputStream inputStream = StringUtils.class.getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new FileNotFoundException("Resource not found: " + resourcePath);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    private static boolean hasRefAttribute(Node keyFragment) {
        return keyFragment.getAttributes() != null && keyFragment.getAttributes().getNamedItem(REF) != null;
    }

    private static void appendRefValue(StringBuilder result, Node keyFragment) {
        String refValue = keyFragment.getAttributes().getNamedItem(REF).getNodeValue();

        if (!result.isEmpty()) {
            result.append(KEY_FRAGMENT_SEPARATOR);
        }

        if (refValue.startsWith(JWT)) {
            // Extract the last part of the string after the last "."
            String lastPart = refValue.substring(refValue.lastIndexOf('.') + 1);
            // Format it as {#context.attributes['jwt.claims']['lastPart']}
            result.append(String.format("{#context.attributes['jwt.claims']['%s']}", lastPart));
        } else {
            // Default behavior
            result.append(String.format(CONTEXT_ATTRIBUTE_FORMAT_WRAPPED, refValue));
        }
    }

    private static void appendTextContent(StringBuilder result, Node keyFragment) {
        if (!result.isEmpty()) {
            result.append(KEY_FRAGMENT_SEPARATOR);
        }
        result.append(keyFragment.getTextContent());
    }
}

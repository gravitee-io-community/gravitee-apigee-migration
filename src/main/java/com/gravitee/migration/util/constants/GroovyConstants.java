package com.gravitee.migration.util.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GroovyConstants {

    public static final String DECODE_JWT_GROOVY = """
            import groovy.json.JsonSlurper
            
            def scope = %s
            def authHeader = scope.headers['Authorization']?.getAt(0)
            def token = authHeader?.replace("Bearer ", "")
            
            if (token) {
                def parts = token.split("\\\\.")
                if (parts.length == 3) {
                    def payloadEncoded = parts[1]
                    def payloadDecoded = new String(payloadEncoded.decodeBase64())
                    def claims = new JsonSlurper().parseText(payloadDecoded)
            
                    // Store the claims in context for downstream usage
                    context.attributes['jwt.claims'] = claims
                }
            }
            """;


    public static final String MESSAGE_VALIDATION_GROOVY = """
            import groovy.json.JsonSlurper
            import groovy.util.XmlSlurper
            
            context.attributes['messagevalidation.failed'] = false
            
            // Get the 'Content-Type' header
            def scope = %s
            def headerValue = scope.headers.get('Content-Type')
            if (headerValue instanceof List) {
                headerValue = headerValue.first()  // Get the first element if it's a List
            }
            
            // Handle JSON content type
            if (headerValue?.toLowerCase().contains("json")) {
                try {
                    def jsonParser = new JsonSlurper()
                    def jsonContent = jsonParser.parseText(scope.content) 
                } catch (Exception e) {
                    // If JSON parsing fails, mark validation as failed
                    context.attributes['messagevalidation.failed'] = true
                }
            } 
            // Handle XML content type
            else if (headerValue?.toLowerCase().contains("xml")) {
                try {
                    def xmlParser = new XmlSlurper()
                    def xmlContent = xmlParser.parseText(scope.content) // Parses XML content
                } catch (Exception e) {
                    // If XML parsing fails, mark validation as failed
                    context.attributes['messagevalidation.failed'] = true
                }
            } 
            
            // Return true only if validation did not fail
            return !context.attributes['messagevalidation.failed']
            """;

    public static final String RAISE_FAULT_GROOVY = """
            import io.gravitee.policy.groovy.PolicyResult.State
            result.state = State.FAILURE
            result.code = %s
            result.error = '{"error":"%s"}'
            result.contentType = '%s'
            """;

    public static final String JSON_TO_XML_GROOVY = """
            import groovy.json.JsonSlurper
            
            // Configurable parameters matching Apigee defaults
            def config = [
                nullValue: "%s",
                rootName: "%s",
                arrayRootName: "%s",
                arrayItemName: "%s",
                attributePrefix: "%s",
                attributeBlock: "%s",
                textNodeName: "%s",
                namespaceBlock: "%s",
                defaultNamespaceNode: "\\%s",
                namespaceSeparator: "%s",
                invalidCharReplacement: "%s",
                omitXmlDeclaration: false,
                indent: false
            ]
            
            try {
                def scope = %s;
                def jsonContent = scope.content?.trim()
                if (!jsonContent) {
                    return false
                }
            
                // Check Content-Type - Apigee requires application/json
                def contentType = scope.headers.get("Content-Type")?.getAt(0)?.toLowerCase()
                if (!contentType?.contains("application/json")) {
                    return false
                }
            
                // Parse JSON
                def json = new JsonSlurper().parseText(jsonContent)
            
                // Generate XML
                def xml = ""
                if (!config.omitXmlDeclaration) {
                    xml += "<?xml version=1.0 encoding=UTF-8?>"
                }
            
                // Root element handling
                if (json instanceof Map) {
                    if (json.size() == 1) {
                        def rootKey = json.keySet().first()
                        def rootValue = json[rootKey]
                        def namespaces = rootValue instanceof Map ? (rootValue[config.namespaceBlock] ?: [:]) : [:]
                        def nsAttrs = buildNamespaceAttributes(namespaces, config)
                        def attributes = [:]
                        if (rootValue instanceof Map) {
                            if (rootValue[config.attributeBlock]) {
                                rootValue[config.attributeBlock].each { k, v ->
                                    attributes[k] = v
                                }
                            }
                            rootValue.each { k, v ->
                                if (k.startsWith(config.attributePrefix)) {
                                    attributes[k.substring(config.attributePrefix.length())] = v
                                }
                            }
                        }
                        def attrStr = attributes.collect { k, v -> " ${k}=${v}" }.join("")
                        if (rootValue instanceof List) {
                            xml += "<${config.arrayRootName}${nsAttrs}${attrStr}>"
                            if (config.indent) xml += ""
                            rootValue.each { item ->
                                def cleanItemKey = cleanElementName(rootKey, namespaces, config)
                                xml += config.indent ? "${"  " * 1}<${cleanItemKey}>" : "<${cleanItemKey}>"
                                if (config.indent && item instanceof Map) xml += ""
                                xml += buildXmlNode(item, config, item instanceof Map ? (item[config.namespaceBlock] ?: namespaces) : [:], config.indent ? 2 : 0)
                                if (config.indent && item instanceof Map) xml += "${"  " * 1}"
                                xml += "</${cleanItemKey}>"
                                if (config.indent) xml += ""
                            }
                            xml += "</${config.arrayRootName}>"
                            if (config.indent) xml += ""
                        } else {
                            def cleanRootKey = cleanElementName(rootKey, namespaces, config)
                            xml += "<${cleanRootKey}${nsAttrs}${attrStr}>"
                            if (config.indent) xml += ""
                            xml += buildXmlNode(rootValue, config, namespaces, 1)
                            xml += "</${cleanRootKey}>"
                            if (config.indent) xml += ""
                        }
                    } else {
                        def namespaces = json[config.namespaceBlock] ?: [:]
                        def nsAttrs = buildNamespaceAttributes(namespaces, config)
                        xml += "<${config.rootName}${nsAttrs}>"
                        if (config.indent) xml += ""
                        xml += buildXmlNode(json, config, namespaces, 1)
                        xml += "</${config.rootName}>"
                        if (config.indent) xml += ""
                    }
                } else if (json instanceof List) {
                    xml += "<${config.arrayRootName}>"
                    if (config.indent) xml += ""
                    json.each { item ->
                        xml += config.indent ? "${"  " * 1}<${config.arrayItemName}>" : "<${config.arrayItemName}>"
                        if (config.indent && item instanceof Map) xml += ""
                        xml += buildXmlNode(item, config, item instanceof Map ? (item[config.namespaceBlock] ?: [:]) : [:], config.indent ? 2 : 0)
                        if (config.indent && item instanceof Map) xml += "${"  " * 1}"
                        xml += "</${config.arrayItemName}>"
                        if (config.indent) xml += ""
                    }
                    xml += "</${config.arrayRootName}>"
                    if (config.indent) xml += ""
                } else {
                    xml += "<${config.rootName}>${json?.toString() ?: ''}</${config.rootName}>"
                    if (config.indent) xml += ""
                }
            
                scope.headers.'Content-Type' = 'text/xml;charset=UTF-8'
                return xml
            
            } catch (Exception e) {
                return false
            }
            
            def buildNamespaceAttributes(namespaces, config) {
                if (!namespaces) return ""
                def result = ""
                def defaultNs = namespaces[config.defaultNamespaceNode]
                if (defaultNs) {
                    result += " xmlns=${defaultNs}"
                }
                namespaces.each { prefix, uri ->
                    if (prefix != config.defaultNamespaceNode) {
                        result += "xmlns:${prefix}=${uri}"
                    }
                }
                return result
            }
            
            String cleanElementName(String key, Map namespaces, Map config) {
                if (key.contains(config.namespaceSeparator)) {
                    String[] parts = key.split(config.namespaceSeparator, 2)
                    if (parts.length == 2) {
                        String prefix = parts[0]
                        String local = parts[1]
                        if (prefix in namespaces) {
                            String cleanLocal = local.replaceAll(/[^a-zA-Z0-9_.-]+/, config.invalidCharReplacement)
                            if (cleanLocal =~ /^[0-9]/) cleanLocal = config.invalidCharReplacement + cleanLocal
                            return "${prefix}:${cleanLocal}"
                        }
                    }
                }
                String cleanKey = key.replaceAll(/[^a-zA-Z0-9_.-]+/, config.invalidCharReplacement)
                if (cleanKey =~ /^[0-9]/) cleanKey = config.invalidCharReplacement + cleanKey
                return cleanKey
            }
            
            String buildXmlNode(Object node, Map config, Map namespaces, int level = 0) {
                def indent = config.indent ? "  " * level : ""
                def result = ""
            
                if (node instanceof Map) {
                    def attributes = [:]
                    def textContent = node[config.textNodeName]
                    def localNamespaces = node[config.namespaceBlock] ?: namespaces
            
                    // Collect attributes from @-prefixed properties
                    node.each { k, v ->
                        if (k.startsWith(config.attributePrefix)) {
                            attributes[k.substring(config.attributePrefix.length())] = v
                        }
                    }
            
                    // Collect attributes from #attrs object
                    if (node[config.attributeBlock]) {
                        node[config.attributeBlock].each { k, v ->
                            attributes[k] = v
                        }
                    }
            
                    def contentMap = node.findAll { k, _ ->
                        k != config.attributeBlock &&
                        k != config.textNodeName &&
                        k != config.namespaceBlock &&
                        !k.startsWith(config.attributePrefix)
                    }
            
                    contentMap.each { key, value ->
                        def cleanKey = cleanElementName(key, localNamespaces, config)
                        if (value instanceof List) {
                            value.each { item ->
                                result += "${indent}<${cleanKey}>"
                                if (config.indent && item instanceof Map) result += ""
                                result += buildXmlNode(item, config, item instanceof Map ? (item[config.namespaceBlock] ?: localNamespaces) : [:], level + 1)
                                if (config.indent && item instanceof Map) result += "${indent}"
                                result += "</${cleanKey}>"
                                if (config.indent) result += ""
                            }
                        } else if (value == null || value == config.nullValue) {
                            result += "${indent}<${cleanKey}></${cleanKey}>${config.indent ? '' : ''}"
                        } else if (value instanceof Map) {
                            def attrStr = attributes.collect { k, v -> " ${k}=${v}" }.join("")
                            result += "${indent}<${cleanKey}${attrStr}>"
                            if (config.indent) result += ""
                            result += buildXmlNode(value, config, localNamespaces, level + 1)
                            result += "${indent}</${cleanKey}>${config.indent ? '' : ''}"
                        } else {
                            result += "${indent}<${cleanKey}>${value}</${cleanKey}>${config.indent ? '' : ''}"
                        }
                    }
            
                    if (textContent != null) {
                        result += "${textContent}${config.indent ? '' : ''}"
                    }
                } else if (node instanceof List) {
                    node.each { item ->
                        result += "${indent}<${config.arrayItemName}>"
                        if (config.indent && item instanceof Map) result += ""
                        result += buildXmlNode(item, config, item instanceof Map ? (item[config.namespaceBlock] ?: namespaces) : [:], level + 1)
                        if (config.indent && item instanceof Map) result += "${indent}"
                        result += "</${config.arrayItemName}>"
                        if (config.indent) result += ""
                    }
                } else if (node != null && node != config.nullValue) {
                    result += "${indent}${node}"
                }
            
                return result
            }
            """;
}

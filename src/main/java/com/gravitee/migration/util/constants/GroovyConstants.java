package com.gravitee.migration.util.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GroovyConstants {

    public static final String DECODE_JWT = """
            import groovy.json.JsonSlurper
            
            def authHeader = request.headers['Authorization']?.getAt(0)
            def token = authHeader?.replace("Bearer ", "")
            
            if (token) {
                def parts = token.split("\\.")
                if (parts.length == 3) {
                    def payloadEncoded = parts[1]
                    def payloadDecoded = new String(payloadEncoded.decodeBase64())
                    def claims = new JsonSlurper().parseText(payloadDecoded)
            
                    // Store the claims in context for downstream usage
                    context.attributes['jwt.claims'] = claims
                }
            }
            """;


    public static final String MESSAGE_VALIDATION = """
            import groovy.json.JsonSlurper
            import groovy.util.XmlSlurper
            
            context.attributes['validationFailed'] = false
            
            // Get the 'Content-Type' header
            def headerValue = request.headers.get('Content-Type')
            if (headerValue instanceof List) {
                headerValue = headerValue.first()  // Get the first element if it's a List
            }
            
            // Handle JSON content type
            if (headerValue?.toLowerCase().contains("json")) {
                try {
                    def jsonParser = new JsonSlurper()
                    def jsonContent = jsonParser.parseText(request.content) 
                } catch (Exception e) {
                    // If JSON parsing fails, mark validation as failed
                    context.attributes['validationFailed'] = true
                }
            } 
            // Handle XML content type
            else if (headerValue?.toLowerCase().contains("xml")) {
                try {
                    def xmlParser = new XmlSlurper()
                    def xmlContent = xmlParser.parseText(request.content) // Parses XML content
                } catch (Exception e) {
                    // If XML parsing fails, mark validation as failed
                    context.attributes['validationFailed'] = true
                }
            } 
            
            // Return true only if validation did not fail
            return !context.attributes['validationFailed']
            """;
}

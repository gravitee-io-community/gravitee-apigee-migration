package com.gravitee.migration.util.constants.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PolicyConstants {

    public static final String FLOW_CALLOUT = "FlowCallout";
    public static final String ASSIGN_MESSAGE = "AssignMessage";
    public static final String EXTRACT_VARIABLES = "ExtractVariables";
    public static final String JSON_TO_XML = "JSONToXML";
    public static final String SPIKE_ARREST = "SpikeArrest";
    public static final String XML_TO_JSON = "XMLToJSON";
    public static final String JAVASCRIPT = "Javascript";
    public static final String XSLT = "XSL";
    public static final String KVM = "KeyValueMapOperations";
    public static final String LOOKUP_CACHE = "LookupCache";
    public static final String POPULATE_CACHE = "PopulateCache";
    public static final String VERIFY_API_KEY = "VerifyAPIKey";
    public static final String MESSAGE_VALIDATION = "MessageValidation";
    public static final String VERIFY_JWT = "VerifyJWT";
    public static final String DECODE_JWT = "DecodeJWT";
    public static final String SERVICE_CALLOUT = "ServiceCallout";
    public static final String RAISE_FAULT = "RaiseFault";
    public static final String ACCESS_ENTITY = "AccessEntity";
    public static final String ROUTING_POLICY = "RoutingPolicy";

}


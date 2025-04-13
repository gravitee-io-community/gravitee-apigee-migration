package com.gravitee.migration.util.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GraviteeCliConstants {

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Api {
        public static final String API_OBJECT = "api";
        public static final String LISTENERS = "listeners";
        public static final String PATHS = "paths";
        public static final String PATH = "path";
        public static final String OVERRIDE_ACCESS = "overrideAccess";
        public static final String HOST = "host";
        public static final String QOS = "qos";
        public static final String ENTRYPOINTS = "entrypoints";
        public static final String ENDPOINT_GROUPS = "endpointGroups";
        public static final String DEFAULT_ENDPOINT = "default-endpoint";
        public static final String LOAD_BALANCER = "loadBalancer";
        public static final String ROUND_ROBIN = "ROUND_ROBIN";
        public static final String SHARED_CONFIGURATION = "sharedConfiguration";
        public static final String ENDPOINTS = "endpoints";
        public static final String WEIGHT = "weight";
        public static final String INHERIT_CONFIGURATION = "inheritConfiguration";
        public static final String TARGET = "target";
        public static final String SECONDARY = "secondary";
        public static final String SSL = "ssl";
        public static final String TRUST_ALL = "trustAll";
        public static final String HOST_NAME_VERIFIER = "hostNameVerifier";
        public static final String KEY_STORE = "keyStore";
        public static final String TRUST_STORE = "trustStore";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Plan {
        public static final String PLANS = "plans";
        public static final String FLOWS = "flows";
        public static final String SELECTORS = "selectors";
        public static final String RULES = "rules";
        public static final String PATTERN = "pattern";
        public static final String SECURITY = "security";
        public static final String API_KEY = "API_KEY";
        public static final String KEY_LESS = "KEY_LESS";
        public static final String STATUS = "status";
        public static final String VALIDATION = "validation";
        public static final String METHOD = "method";
        public static final String VARIABLES = "variables";
        public static final String FIRE_AND_FORGET = "fireAndForget";
        public static final String EXIT_ON_ERROR = "exitOnError";
        public static final String ERROR_CONDITION = "errorCondition";
        public static final String ERROR_STATUS_CODE = "errorStatusCode";
        public static final String ADD_HEADERS = "addHeaders";
        public static final String PROPAGATE_API_KEY = "propagateApiKey";
        public static final String X_GRAVITEE_API_KEY = "X-Gravitee-Api-Key";
        public static final String STYLESHEET = "stylesheet";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Policy {
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
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class PolicyType {
        public static final String GROOVY = "groovy";
        public static final String ASSIGN_ATTRIBUTES = "policy-assign-attributes";
        public static final String JAVASCRIPT = "javascript";
        public static final String HTTP_CALLOUT = "policy-http-callout";
        public static final String TRANSFORM_HEADERS = "transform-headers";
        public static final String API_KEY = "api-key";
        public static final String XSLT = "xslt";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Common {
        public static final String DEFINITION_VERSION = "definitionVersion";
        public static final String V4 = "V4";
        public static final String TYPE = "type";
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String DESCRIPTION = "description";
        public static final String API_VERSION = "apiVersion";
        public static final String CREATED_AT = "createdAt";
        public static final String UPDATED_AT = "updatedAt";
        public static final String CONFIGURATION = "configuration";
        public static final String HTTP_PROXY = "http-proxy";
        public static final String ENABLED = "enabled";
        public static final String PROXY = "proxy";
        public static final String HTTP = "HTTP";
        public static final String AUTO = "AUTO";
        public static final String SERVICES = "services";
        public static final String REQUEST = "request";
        public static final String RESPONSE = "response";
        public static final String ASYNC = "async";
        public static final String POLICY = "policy";
        public static final String URL = "url";
        public static final String VALUE = "value";
        public static final String PARAMETERS = "parameters";
        public static final String ATTRIBUTES = "attributes";
        public static final String SCRIPT = "script";
        public static final String SCOPE = "scope";
        public static final String READ_CONTENT = "readContent";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Folder {
        public static final String API_PROXY = "apiproxy";
        public static final String PROXIES = "proxies";
        public static final String POLICIES = "policies";
        public static final String SHARED_FLOW_BUNDLE = "sharedflowbundle";
        public static final String SHARED_FLOWS = "sharedflows";
        public static final String RESOURCES = "resources";
        public static final String TARGETS = "targets";
        public static final String XSLT = "xslt";
    }
}

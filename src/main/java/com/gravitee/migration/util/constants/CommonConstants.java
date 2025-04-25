package com.gravitee.migration.util.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonConstants {

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
    public static final String OVERRIDE_CONTENT = "overrideContent";
    public static final String APPLICATION_JSON = "application/json";
    public static final String LIMIT = "limit";
    public static final String PERIOD_TIME = "periodTime";
    public static final String PERIOD_TIME_UNIT = "periodTimeUnit";
    public static final String KEY = "key";
    public static final String REQUEST_CONTENT = "#request.content";
    public static final String RESPONSE_CONTENT = "#response.content";
    public static final String REQUEST_CONTENT_WRAPPED = "{#request.content}";
    public static final String RESPONSE_CONTENT_WRAPPED = "{#response.content}";
    public static final String DICTIONARY_FORMAT = "#dictionaries['%s']['%s']";
    public static final String DICTIONARY_FORMAT_WRAPPED = "{#dictionaries['%s']['%s']}";
    public static final String CONTEXT_ATTRIBUTE_FORMAT = "#context.attributes['%s']";
    public static final String CONTEXT_ATTRIBUTE_FORMAT_WRAPPED = "{#context.attributes['%s']}";
    public static final String REQUEST_HEADER_WRAPPED = "{#request.headers['%s']}";
    public static final String API_PROXY_NAME = "apiproxy.name";
    public static final String API = "api";
    public static final String CHANGE_ME = "CHANGE_ME";
    public static final String RESOURCE = "resource";
    public static final String HEADERS = "headers";
    public static final String BODY = "body";
    public static final String PUBLISHED = "PUBLISHED";
    public static final String STAGING = "STAGING";
    public static final String MANUAL = "MANUAL";
    public static final String REF = "ref";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String REMOVE_HEADERS = "removeHeaders";
    public static final String CONDITION = "condition";
    public static final String PATH = "path";
    public static final String PATH_OPERATOR = "pathOperator";
    public static final String STARTS_WITH = "STARTS_WITH";
    public static final String EQUALS_PATH_OPERATOR = "EQUALS";
    public static final String METHODS = "methods";

}
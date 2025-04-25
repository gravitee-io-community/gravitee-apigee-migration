package com.gravitee.migration.util.constants.object;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlanObjectConstants {

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
    public static final String ON_REQUEST_SCRIPT = "onRequestScript";
    public static final String ON_RESPONSE_SCRIPT = "onResponseScript";
    public static final String PUBLIC_KEY_RESOLVER = "publicKeyResolver";
    public static final String JWKS_CONFIG = "jwksConfig";
    public static final String RESOLVER_PARAMETER = "resolverParameter";
    public static final String SIGNATURE = "signature";
    public static final String EXTRACT_CLAIMS = "extractClaims";
    public static final String PROPAGATE_AUTHORIZATION_HEADER = "propagateAuthorizationHeader";
    public static final String SPIKE = "spike";
    public static final String CACHE_KEY = "cacheKey";
    public static final String DEFAULT_OPERATION = "defaultOperation";
    public static final String CALLOUT_RESPONSE_CONTENT = "{#calloutResponse.content}";
    public static final String ERROR_CONDITION_EXPRESSION = "{#calloutResponse.status >= 400 and #calloutResponse.status <= 599}";
    public static final String GIVEN_KEY = "GIVEN_KEY";
    public static final String CONNECT_TIMEOUT = "connectTimeout";
    public static final String REQUEST_TIMEOUT = "requestTimeout";
    public static final String FOLLOW_REDIRECTS = "followRedirects";
    public static final String USE_SYSTEM_PROXY = "useSystemProxy";
    public static final String PROPAGATE_AUTH_HEADER = "propagateAuthHeader";
    public static final String USER_CLAIM = "userClaim";
    public static final String SUB = "sub";
    public static final String IGNORE_MISSING = "ignoreMissing";
    public static final String CERTIFICATE_BOUND_THUMBPRINT = "certificateBoundThumbprint";
    public static final String EXTRACT_CERTIFICATE_FROM_HEADER = "extractCertificateFromHeader";
    public static final String HEADER_NAME = "headerName";
    public static final String SSL_CLIENT_CERT = "ssl-client-cert";
    public static final String EXPECTED_VALUES = "expectedValues";
    public static final String IGNORE_CASE = "ignoreCase";
    public static final String CONFIRMATION_METHOD_VALIDATION = "confirmationMethodValidation";
    public static final String TOKEN_TYP_VALIDATION = "tokenTypValidation";
    public static final String SHARED_POLICY_GROUP_ID = "sharedPolicyGroupId";

}
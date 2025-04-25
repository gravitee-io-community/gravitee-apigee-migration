package com.gravitee.migration.util.constants.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PolicyTypeConstants {

    public static final String GROOVY = "groovy";
    public static final String ASSIGN_ATTRIBUTES = "policy-assign-attributes";
    public static final String JAVASCRIPT = "javascript";
    public static final String HTTP_CALLOUT = "policy-http-callout";
    public static final String TRANSFORM_HEADERS = "transform-headers";
    public static final String API_KEY = "api-key";
    public static final String XSLT = "xslt";
    public static final String JWT = "jwt";
    public static final String SPIKE_ARREST = "spike-arrest";
    public static final String DATA_CACHE = "data-cache";
    public static final String DYNAMIC_ROUTING = "dynamic-routing";
    public static final String OVERRIDE_REQUEST_METHOD = "policy-override-request-method";
    public static final String ASSIGN_CONTENT = "policy-assign-content";
    public static final String SHARED_POLICY_GROUP = "shared-policy-group-policy";
}

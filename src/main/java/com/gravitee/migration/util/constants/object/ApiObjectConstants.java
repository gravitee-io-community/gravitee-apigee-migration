package com.gravitee.migration.util.constants.object;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiObjectConstants {

    public static final String API_OBJECT = "api";
    public static final String LISTENERS = "listeners";
    public static final String PATHS = "paths";
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
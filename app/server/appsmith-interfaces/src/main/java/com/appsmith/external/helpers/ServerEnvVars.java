package com.appsmith.external.helpers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides server-side environment variables for mustache substitution in
 * backend-executed queries.
 *
 * <p>Only system env vars whose names start with a configurable prefix
 * (default {@code APPSMITH_ENV_}) are exposed. The prefix is stripped when
 * building the substitution map so that {@code {{env.PG_KEY}}} resolves to
 * the value of system env var {@code APPSMITH_ENV_PG_KEY}.
 *
 * <p>The prefix itself is configurable via the system env var
 * {@code APPSMITH_ENV_VAR_PREFIX}.
 */
public final class ServerEnvVars {

    static final String DEFAULT_PREFIX = "APPSMITH_ENV_";
    static final String BINDING_NAMESPACE = "env.";

    private static final String PREFIX = resolvePrefix();

    private static final Map<String, String> CACHED_MAP = buildSubstitutionMap();

    private ServerEnvVars() {}

    /**
     * Returns an immutable substitution map built from system env vars whose
     * names start with the configured prefix.
     *
     * <p>Example: env var {@code APPSMITH_ENV_PG_KEY=secret} produces
     * map entry {@code "env.PG_KEY" → "secret"}.
     */
    public static Map<String, String> getSubstitutionMap() {
        return CACHED_MAP;
    }

    /**
     * Returns {@code true} if the given mustache key references a
     * server-side env var (i.e. starts with {@code "env."}).
     */
    public static boolean isEnvVarKey(String key) {
        return key != null && key.startsWith(BINDING_NAMESPACE);
    }

    private static String resolvePrefix() {
        String custom = System.getenv("APPSMITH_ENV_VAR_PREFIX");
        return (custom != null && !custom.isBlank()) ? custom : DEFAULT_PREFIX;
    }

    private static Map<String, String> buildSubstitutionMap() {
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            if (entry.getKey().startsWith(PREFIX) && entry.getKey().length() > PREFIX.length()) {
                String shortKey = BINDING_NAMESPACE + entry.getKey().substring(PREFIX.length());
                map.put(shortKey, entry.getValue());
            }
        }
        return Collections.unmodifiableMap(map);
    }
}

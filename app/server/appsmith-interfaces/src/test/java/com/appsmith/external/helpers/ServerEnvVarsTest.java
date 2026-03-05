package com.appsmith.external.helpers;

import com.appsmith.external.models.ActionConfiguration;
import com.appsmith.external.models.DatasourceConfiguration;
import com.appsmith.external.models.Endpoint;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ServerEnvVarsTest {

    @Test
    void isEnvVarKey_withEnvPrefix_returnsTrue() {
        assertThat(ServerEnvVars.isEnvVarKey("env.PG_KEY")).isTrue();
        assertThat(ServerEnvVars.isEnvVarKey("env.X")).isTrue();
    }

    @Test
    void isEnvVarKey_withoutEnvPrefix_returnsFalse() {
        assertThat(ServerEnvVars.isEnvVarKey("Input1.text")).isFalse();
        assertThat(ServerEnvVars.isEnvVarKey("Api1.data")).isFalse();
        assertThat(ServerEnvVars.isEnvVarKey("environment.PG_KEY")).isFalse();
        assertThat(ServerEnvVars.isEnvVarKey(null)).isFalse();
        assertThat(ServerEnvVars.isEnvVarKey("")).isFalse();
    }

    @Test
    void getSubstitutionMap_returnsImmutableMap() {
        Map<String, String> map = ServerEnvVars.getSubstitutionMap();
        assertThat(map).isNotNull();
        // Must be unmodifiable
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class, () -> map.put("env.TEST", "value"));
    }

    @Test
    void renderFieldValues_substitutesEnvVarsInActionBody() {
        Map<String, String> envMap = Map.of("env.PG_KEY", "my-secret");

        ActionConfiguration config = new ActionConfiguration();
        config.setBody("SELECT pgp_sym_decrypt(col, '{{env.PG_KEY}}') FROM users");

        MustacheHelper.renderFieldValues(config, envMap);

        assertThat(config.getBody()).isEqualTo("SELECT pgp_sym_decrypt(col, 'my-secret') FROM users");
    }

    @Test
    void renderFieldValues_leavesNonEnvBindingsUntouched() {
        Map<String, String> envMap = Map.of("env.TOKEN", "tok_123");

        ActionConfiguration config = new ActionConfiguration();
        config.setBody("SELECT * FROM {{Table1.name}} WHERE key = '{{env.TOKEN}}'");

        MustacheHelper.renderFieldValues(config, envMap);

        assertThat(config.getBody()).isEqualTo("SELECT * FROM {{Table1.name}} WHERE key = 'tok_123'");
    }

    @Test
    void renderFieldValues_substitutesEnvVarsInDatasourceEndpoint() {
        Map<String, String> envMap = Map.of("env.DB_HOST", "prod-db.internal");

        DatasourceConfiguration dsConfig = new DatasourceConfiguration();
        Endpoint endpoint = new Endpoint();
        endpoint.setHost("{{env.DB_HOST}}");
        endpoint.setPort(5432L);
        dsConfig.setEndpoints(List.of(endpoint));

        MustacheHelper.renderFieldValues(dsConfig, envMap);

        assertThat(dsConfig.getEndpoints().get(0).getHost()).isEqualTo("prod-db.internal");
    }

    @Test
    void renderFieldValues_handlesMultipleEnvVarsInSameString() {
        Map<String, String> envMap = Map.of(
                "env.SCHEMA", "analytics",
                "env.TABLE", "events");

        ActionConfiguration config = new ActionConfiguration();
        config.setBody("SELECT * FROM {{env.SCHEMA}}.{{env.TABLE}}");

        MustacheHelper.renderFieldValues(config, envMap);

        assertThat(config.getBody()).isEqualTo("SELECT * FROM analytics.events");
    }

    @Test
    void renderFieldValues_noOpWhenNoEnvBindings() {
        Map<String, String> envMap = Map.of("env.UNUSED", "value");

        ActionConfiguration config = new ActionConfiguration();
        config.setBody("SELECT * FROM users WHERE id = {{Input1.text}}");

        MustacheHelper.renderFieldValues(config, envMap);

        assertThat(config.getBody()).isEqualTo("SELECT * FROM users WHERE id = {{Input1.text}}");
    }

    @Test
    void renderFieldValues_emptyEnvMap_isNoOp() {
        Map<String, String> envMap = Map.of();

        ActionConfiguration config = new ActionConfiguration();
        config.setBody("SELECT '{{env.PG_KEY}}'");

        MustacheHelper.renderFieldValues(config, envMap);

        assertThat(config.getBody()).isEqualTo("SELECT '{{env.PG_KEY}}'");
    }
}

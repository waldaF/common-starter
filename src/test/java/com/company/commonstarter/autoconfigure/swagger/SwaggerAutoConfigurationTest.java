package com.company.commonstarter.autoconfigure.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import com.company.commonstarter.autoconfigure.SwaggerAutoConfiguration;
import com.company.commonstarter.swagger.SwaggerProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SwaggerAutoConfiguration.class));

    private static final String ENABLED = "company.swagger.enabled=true";
    private static final String TEAM = "company.swagger.team=Platform Team";
    private static final String TEAM_URL = "company.swagger.team-url=https://example.com/team";

    @Test
    void disabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(SwaggerProperties.class);
            assertThat(context).doesNotHaveBean(OpenAPI.class);
        });
    }

    @Test
    void failsToStartWhenEnabledWithoutRequiredTeamFields() {
        contextRunner.withPropertyValues(ENABLED)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context).getFailure()
                            .hasRootCauseInstanceOf(BindValidationException.class)
                            .rootCause()
                            .hasMessageContaining("Team name cannot be blank")
                            .hasMessageContaining("Team url can not be blank");
                });
    }

    @Test
    void createsOpenApiBeanWhenEnabled() {
        contextRunner.withPropertyValues(ENABLED, TEAM, TEAM_URL)
                .run(context -> {
                    assertThat(context).hasSingleBean(OpenAPI.class);
                    final OpenAPI openApi = context.getBean(OpenAPI.class);
                    assertThat(openApi.getInfo().getContact().getName()).isEqualTo("Platform Team");
                    assertThat(openApi.getInfo().getContact().getUrl()).isEqualTo("https://example.com/team");
                });
    }

    @Test
    void omitsServersWhenServerUrlBlank() {
        contextRunner.withPropertyValues(ENABLED, TEAM, TEAM_URL)
                .run(context -> assertThat(context.getBean(OpenAPI.class).getServers()).isNullOrEmpty());
    }

    @Test
    void addsServerWhenServerUrlProvided() {
        contextRunner.withPropertyValues(ENABLED, TEAM, TEAM_URL,
                        "company.swagger.server-url=https://service.internal")
                .run(context -> assertThat(context.getBean(OpenAPI.class).getServers())
                        .extracting("url")
                        .containsExactly("https://service.internal"));
    }

    @Test
    void addsNoSecuritySchemeWhenAuthTypeNone() {
        contextRunner.withPropertyValues(ENABLED, TEAM, TEAM_URL)
                .run(context -> {
                    final OpenAPI openApi = context.getBean(OpenAPI.class);
                    assertThat(openApi.getComponents()).isNull();
                    assertThat(openApi.getSecurity()).isNullOrEmpty();
                });
    }

    @Test
    void configuresBearerSchemeForJwtAuthType() {
        contextRunner.withPropertyValues(ENABLED, TEAM, TEAM_URL,
                        "company.swagger.auth-type=JWT")
                .run(context -> {
                    final OpenAPI openApi = context.getBean(OpenAPI.class);
                    final SecurityScheme scheme = openApi.getComponents().getSecuritySchemes().get("bearer-auth");
                    assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
                    assertThat(scheme.getScheme()).isEqualTo("bearer");
                    assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
                    assertThat(openApi.getSecurity()).anyMatch(req -> req.containsKey("bearer-auth"));
                });
    }

    @Test
    void configuresBasicSchemeForBasicAuthType() {
        contextRunner.withPropertyValues(ENABLED, TEAM, TEAM_URL,
                        "company.swagger.auth-type=BASIC")
                .run(context -> {
                    final SecurityScheme scheme = context.getBean(OpenAPI.class)
                            .getComponents().getSecuritySchemes().get("basic-auth");
                    assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
                    assertThat(scheme.getScheme()).isEqualTo("basic");
                });
    }

    @Test
    void configuresOAuth2SchemeWithTokenUrlForOAuth2AuthType() {
        contextRunner.withPropertyValues(ENABLED, TEAM, TEAM_URL,
                        "company.swagger.auth-type=OAUTH2",
                        "company.swagger.oauth2-token-url=https://auth.example.com/token")
                .run(context -> {
                    final SecurityScheme scheme = context.getBean(OpenAPI.class)
                            .getComponents().getSecuritySchemes().get("oauth2-auth");
                    assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.OAUTH2);
                    assertThat(scheme.getFlows().getClientCredentials().getTokenUrl())
                            .isEqualTo("https://auth.example.com/token");
                });
    }

    @Test
    void configuresApiKeySchemeWithHeaderNameForApiKeyAuthType() {
        contextRunner.withPropertyValues(ENABLED, TEAM, TEAM_URL,
                        "company.swagger.auth-type=API_KEY",
                        "company.swagger.api-key-header-name=X-Custom-Key")
                .run(context -> {
                    final SecurityScheme scheme = context.getBean(OpenAPI.class)
                            .getComponents().getSecuritySchemes().get("api-key-auth");
                    assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.APIKEY);
                    assertThat(scheme.getIn()).isEqualTo(SecurityScheme.In.HEADER);
                    assertThat(scheme.getName()).isEqualTo("X-Custom-Key");
                });
    }

    @Test
    void usesBuildPropertiesWhenAvailable() {
        contextRunner.withPropertyValues(ENABLED, TEAM, TEAM_URL)
                .withUserConfiguration(BuildPropertiesConfig.class)
                .run(context -> {
                    final OpenAPI openApi = context.getBean(OpenAPI.class);
                    assertThat(openApi.getInfo().getTitle()).isEqualTo("e2e-service");
                    assertThat(openApi.getInfo().getVersion()).isEqualTo("1.2.3");
                });
    }

    @Test
    void toleratesMissingBuildProperties() {
        contextRunner.withPropertyValues(ENABLED, TEAM, TEAM_URL)
                .run(context -> assertThat(context.getBean(OpenAPI.class).getInfo().getTitle()).isNull());
    }

    @Test
    void backsOffWhenConsumerDefinesOwnOpenApiBean() {
        contextRunner.withPropertyValues(ENABLED, TEAM, TEAM_URL)
                .withUserConfiguration(CustomOpenApiConfig.class)
                .run(context -> assertThat(context.getBean(OpenAPI.class))
                        .isSameAs(CustomOpenApiConfig.CUSTOM_OPEN_API));
    }

    @Configuration
    static class BuildPropertiesConfig {

        @Bean
        BuildProperties buildProperties() {
            final Properties properties = new Properties();
            properties.put("name", "e2e-service");
            properties.put("version", "1.2.3");
            return new BuildProperties(properties);
        }
    }

    @Configuration
    static class CustomOpenApiConfig {

        static final OpenAPI CUSTOM_OPEN_API = new OpenAPI();

        @Bean
        OpenAPI commonStarterOpenApi() {
            return CUSTOM_OPEN_API;
        }
    }
}
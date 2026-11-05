package com.company.commonstarter.autoconfigure;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import com.company.commonstarter.swagger.SwaggerAuthType;
import com.company.commonstarter.swagger.SwaggerProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
@ConditionalOnProperty(prefix = "company.swagger", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(SwaggerProperties.class)
public class SwaggerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI commonStarterOpenApi(final SwaggerProperties properties,
                                        final ObjectProvider<BuildProperties> buildPropertiesProvider) {
        final BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        final OpenAPI openApi = new OpenAPI();
        openApi.info(new Info()
                .title(Objects.nonNull(buildProperties) ? buildProperties.getName() : null)
                .description(Objects.nonNull(buildProperties) ? buildProperties.get("description") : null)
                .version(Objects.nonNull(buildProperties) ? buildProperties.getVersion() : null)
                .contact(new Contact()
                        .name(properties.getTeam())
                        .url(properties.getTeamUrl())
                )
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"))
        );
        if (StringUtils.hasText(properties.getServerUrl())) {
            openApi.servers(List.of(
                    new Server()
                            .url(properties.getServerUrl())
                            .description("API server")
            ));
        }
        final SecurityScheme securityScheme = toSecurityScheme(properties);
        if (Objects.nonNull(securityScheme)) {
            final String securitySchemeName = securitySchemeName(properties.getAuthType());
            openApi.components(new Components()
                            .addSecuritySchemes(securitySchemeName, securityScheme)
                    )
                    .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
        }
        return openApi;
    }

    private static SecurityScheme toSecurityScheme(final SwaggerProperties properties) {
        return switch (properties.getAuthType()) {
            case NONE -> null;
            case JWT -> new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT");
            case BASIC -> new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic");
            case OAUTH2 -> new SecurityScheme().type(SecurityScheme.Type.OAUTH2)
                    .flows(new OAuthFlows().clientCredentials(
                            new OAuthFlow().tokenUrl(properties.getOauth2TokenUrl())));
            case API_KEY -> new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name(properties.getApiKeyHeaderName());
        };
    }

    private static String securitySchemeName(final SwaggerAuthType authType) {
        return switch (authType) {
            case NONE -> null;
            case JWT -> "bearer-auth";
            case BASIC -> "basic-auth";
            case OAUTH2 -> "oauth2-auth";
            case API_KEY -> "api-key-auth";
        };
    }
}
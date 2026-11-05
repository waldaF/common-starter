package com.company.commonstarter.autoconfigure;

import io.jsonwebtoken.Jwts;
import com.company.commonstarter.config.JwtClientProperties;
import com.company.commonstarter.jwt.client.JwtClientInterceptor;
import com.company.commonstarter.jwt.client.JwtClientSettings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@AutoConfiguration
@ConditionalOnClass({Jwts.class, RestClient.class})
@ConditionalOnProperty(prefix = "company.jwt", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(JwtClientProperties.class)
public class JwtClientAutoConfiguration {

    // One interceptor per named client, keyed by name, for services calling several targets that trust different secrets.
    @Bean
    @ConditionalOnMissingBean(name = "jwtClientInterceptors")
    public Map<String, JwtClientInterceptor> jwtClientInterceptors(final JwtClientProperties properties) {
        return properties.getClients().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> {
                            var client = entry.getValue();
                            var settings = new JwtClientSettings(
                                    client.getSecret(),
                                    client.getExpirationInMinutes(),
                                    client.getExpirationRefreshBeforeInMinutes()
                            );
                            return new JwtClientInterceptor(settings, entry.getKey(), client.getAuthScheme());
                        },
                        (existing, _) -> existing,
                        LinkedHashMap::new
                ));
    }
}
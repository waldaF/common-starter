package com.company.commonstarter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.net.ssl.HttpsURLConnection;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@ConfigurationProperties(prefix = "company.logging")
public class LoggingProperties {

    private boolean enabled = true;
    private boolean simpleConsoleLogging = false;
    private String env = "local";
    private String appName = "";
    private int maxBodyChars = 1000 * 1024;
    private int maxOutboundBodyChars = 500 * 1024;
    private List<String> excludePathPatterns = FrameworkPaths.DEFAULT;
    private Set<Integer> errorStatusCodes = Set.of(HttpsURLConnection.HTTP_UNAUTHORIZED, HttpsURLConnection.HTTP_FORBIDDEN);
}
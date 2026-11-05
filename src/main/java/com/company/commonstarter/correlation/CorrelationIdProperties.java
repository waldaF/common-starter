package com.company.commonstarter.correlation;

import com.company.commonstarter.mdc.MdcKeys;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "company.correlation-id")
public class CorrelationIdProperties {

    private boolean enabled = true;
    private String headerName = "X-Correlation-Id";
    private String mdcKey = MdcKeys.CORRELATION_ID;
}
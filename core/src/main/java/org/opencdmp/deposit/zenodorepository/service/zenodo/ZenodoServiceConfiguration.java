package org.opencdmp.deposit.zenodorepository.service.zenodo;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ZenodoServiceProperties.class})
public class ZenodoServiceConfiguration {
}

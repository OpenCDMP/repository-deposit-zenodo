package org.opencdmp.deposit.zenodorepository.configuration;

import org.opencdmp.deposit.zenodorepository.configuration.funder.FunderProperties;
import org.opencdmp.deposit.zenodorepository.configuration.identifier.IdentifierProperties;
import org.opencdmp.deposit.zenodorepository.configuration.pid.PidProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({PidProperties.class, FunderProperties.class, IdentifierProperties.class})
public class GenericConfiguration {
}

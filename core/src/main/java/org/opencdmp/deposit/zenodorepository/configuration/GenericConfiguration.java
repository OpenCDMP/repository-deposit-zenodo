package org.opencdmp.deposit.zenodorepository.configuration;

import org.opencdmp.deposit.zenodorepository.configuration.funder.FunderProperties;
import org.opencdmp.deposit.zenodorepository.configuration.programminglanguages.ProgrammingLanguagesProperties;
import org.opencdmp.deposit.zenodorepository.configuration.semantics.SemanticsProperties;
import org.opencdmp.deposit.zenodorepository.configuration.pid.PidProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({PidProperties.class, FunderProperties.class, SemanticsProperties.class, ProgrammingLanguagesProperties.class})
public class GenericConfiguration {
}

package org.opencdmp.deposit.zenodorepository.service.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({FileStorageServiceProperties.class})
public class FileStorageServiceConfiguration {
}

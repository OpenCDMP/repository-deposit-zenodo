package org.opencdmp.deposit.zenodorepository.configuration.identifier;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "identifiers")
public class IdentifierProperties {
    private List<String> related;

    public List<String> getRelated() {
        return related;
    }

    public void setRelated(List<String> related) {
        this.related = related;
    }
}

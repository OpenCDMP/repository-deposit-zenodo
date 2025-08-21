package org.opencdmp.deposit.zenodorepository.configuration.semantics;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "semantics")
public class SemanticsProperties {
    private List<String> relatedIdentifiers;

    private List<String> relatedIdentifiersScheme;

    private List<String> dateTypes;

    private List<String> alternateIdentifiersScheme;

    private List<String> conference;

    public List<String> getRelatedIdentifiers() {
        return relatedIdentifiers;
    }

    public void setRelatedIdentifiers(List<String> relatedIdentifiers) {
        this.relatedIdentifiers = relatedIdentifiers;
    }

    public List<String> getRelatedIdentifiersScheme() {
        return relatedIdentifiersScheme;
    }

    public void setRelatedIdentifiersScheme(List<String> relatedIdentifiersScheme) {
        this.relatedIdentifiersScheme = relatedIdentifiersScheme;
    }

    public List<String> getDateTypes() {
        return dateTypes;
    }

    public void setDateTypes(List<String> dateTypes) {
        this.dateTypes = dateTypes;
    }

    public List<String> getAlternateIdentifiersScheme() {
        return alternateIdentifiersScheme;
    }

    public void setAlternateIdentifiersScheme(List<String> alternateIdentifiersScheme) {
        this.alternateIdentifiersScheme = alternateIdentifiersScheme;
    }

    public List<String> getConference() {
        return conference;
    }

    public void setConference(List<String> conference) {
        this.conference = conference;
    }
}

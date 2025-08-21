package org.opencdmp.deposit.zenodorepository.configuration.programminglanguages;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "programming-languages")
public class ProgrammingLanguagesProperties {

    private List<String> programmingCodes;

    private List<String> developmentStatus;

    public List<String> getProgrammingCodes() {
        return programmingCodes;
    }

    public void setProgrammingCodes(List<String> programmingCodes) {
        this.programmingCodes = programmingCodes;
    }

    public List<String> getDevelopmentStatus() {
        return developmentStatus;
    }

    public void setDevelopmentStatus(List<String> developmentStatus) {
        this.developmentStatus = developmentStatus;
    }
}
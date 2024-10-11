package org.opencdmp.deposit.zenodorepository.service.zenodo;

import org.opencdmp.depositbase.repository.DepositConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zenodo")
public class ZenodoServiceProperties {
    private String logo;

    private String community;

    private String domain;

    private String affiliation;

    private DepositConfiguration depositConfiguration;

    private String organizationReferenceCode;
    private String grantReferenceCode;
    private String funderReferenceCode;
    private String researcherReferenceCode;
    private String licensesReferenceCode;
    private String openaireGrantSourceCode;
    private String orcidResearcherSourceCode;
    private int maxInMemorySizeInBytes;

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getCommunity() {
        return community;
    }

    public void setCommunity(String community) {
        this.community = community;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public DepositConfiguration getDepositConfiguration() {
        return depositConfiguration;
    }

    public void setDepositConfiguration(DepositConfiguration depositConfiguration) {
        this.depositConfiguration = depositConfiguration;
    }

    public String getOrganizationReferenceCode() {
        return organizationReferenceCode;
    }

    public void setOrganizationReferenceCode(String organizationReferenceCode) {
        this.organizationReferenceCode = organizationReferenceCode;
    }

    public String getGrantReferenceCode() {
        return grantReferenceCode;
    }

    public void setGrantReferenceCode(String grantReferenceCode) {
        this.grantReferenceCode = grantReferenceCode;
    }

    public String getFunderReferenceCode() {
        return funderReferenceCode;
    }

    public void setFunderReferenceCode(String funderReferenceCode) {
        this.funderReferenceCode = funderReferenceCode;
    }

    public String getResearcherReferenceCode() {
        return researcherReferenceCode;
    }

    public void setResearcherReferenceCode(String researcherReferenceCode) {
        this.researcherReferenceCode = researcherReferenceCode;
    }

    public String getLicensesReferenceCode() {
        return licensesReferenceCode;
    }

    public void setLicensesReferenceCode(String licensesReferenceCode) {
        this.licensesReferenceCode = licensesReferenceCode;
    }

    public String getOpenaireGrantSourceCode() {
        return openaireGrantSourceCode;
    }

    public void setOpenaireGrantSourceCode(String openaireGrantSourceCode) {
        this.openaireGrantSourceCode = openaireGrantSourceCode;
    }

    public String getOrcidResearcherSourceCode() {
        return orcidResearcherSourceCode;
    }

    public void setOrcidResearcherSourceCode(String orcidResearcherSourceCode) {
        this.orcidResearcherSourceCode = orcidResearcherSourceCode;
    }

    public int getMaxInMemorySizeInBytes() {
        return maxInMemorySizeInBytes;
    }

    public void setMaxInMemorySizeInBytes(int maxInMemorySizeInBytes) {
        this.maxInMemorySizeInBytes = maxInMemorySizeInBytes;
    }
}

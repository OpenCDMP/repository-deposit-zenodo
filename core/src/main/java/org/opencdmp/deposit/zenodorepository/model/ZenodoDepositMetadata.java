package org.opencdmp.deposit.zenodorepository.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opencdmp.deposit.zenodorepository.enums.ZenodoAccessRight;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ZenodoDepositMetadata {

    private String title;

    @JsonProperty("upload_type")
    private String uploadType;

    @JsonProperty("publication_type")
    private String publicationType;

    private String description;

    private String version;

    @JsonProperty("publication_date")
    private String publicationDate;

    private List<String> keywords;

    private String notes;

    private List<String> references;

    private List<ZenodoCommunity> communities;

    @JsonProperty("access_right")
    private ZenodoAccessRight accessRight;

    @JsonProperty("access_conditions")
    private String accessConditions;

    @JsonProperty("embargo_date")
    private String embargoDate;

    private String license;

    @JsonProperty("related_identifiers")
    private List<ZenodoRelator> relatedIdentifiers;

    private List<ZenodoContributor> contributors;

    private List<ZenodoGrant> grants;

    private List<ZenodoCreator> creators;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUploadType() {
        return uploadType;
    }

    public void setUploadType(String uploadType) {
        this.uploadType = uploadType;
    }

    public String getPublicationType() {
        return publicationType;
    }

    public void setPublicationType(String publicationType) {
        this.publicationType = publicationType;
    }

    public String getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<String> getReferences() {
        return references;
    }

    public void setReferences(List<String> references) {
        this.references = references;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<ZenodoCommunity> getCommunities() {
        return communities;
    }

    public void setCommunities(List<ZenodoCommunity> communities) {
        this.communities = communities;
    }

    public ZenodoAccessRight getAccessRight() {
        return accessRight;
    }

    public void setAccessRight(ZenodoAccessRight accessRight) {
        this.accessRight = accessRight;
    }

    public String getAccessConditions() {
        return accessConditions;
    }

    public void setAccessConditions(String accessConditions) {
        this.accessConditions = accessConditions;
    }

    public String getEmbargoDate() {
        return embargoDate;
    }

    public void setEmbargoDate(String embargoDate) {
        this.embargoDate = embargoDate;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public List<ZenodoRelator> getRelatedIdentifiers() {
        return relatedIdentifiers;
    }

    public void setRelatedIdentifiers(List<ZenodoRelator> relatedIdentifiers) {
        this.relatedIdentifiers = relatedIdentifiers;
    }

    public List<ZenodoContributor> getContributors() {
        return contributors;
    }

    public void setContributors(List<ZenodoContributor> contributors) {
        this.contributors = contributors;
    }

    public List<ZenodoGrant> getGrants() {
        return grants;
    }

    public void setGrants(List<ZenodoGrant> grants) {
        this.grants = grants;
    }

    public List<ZenodoCreator> getCreators() {
        return creators;
    }

    public void setCreators(List<ZenodoCreator> creators) {
        this.creators = creators;
    }
}

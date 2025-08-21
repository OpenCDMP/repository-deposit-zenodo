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

    private String description;

    private String version;

    @JsonProperty("publication_date")
    private String publicationDate;

    @JsonProperty("rights")
    private List<ZenodoLicense> license;

    private List<ZenodoDates> dates;

    @JsonProperty("identifiers")
    private List<ZenodoAlternateIdentifiers> alternateIdentifiers;

    @JsonProperty("related_identifiers")
    private List<ZenodoRelator> relatedIdentifiers;

    private List<ZenodoCreator> contributors;

    private List<ZenodoCreator> creators;

    private String publisher;

    @JsonProperty("resource_type")
    private ZenodoResourceType resourceType;

    private List<ZenodoFunding> funding;

    private List<ZenodoSubject> subjects;

    private List<ZenodoLanguage> languages;

    @JsonProperty("additional_titles")
    private List<ZenodoAdditionalTitle> additionalTitles;

    private List<ZenodoReference> references;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public String getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
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

    public List<ZenodoLicense> getLicense() {
        return license;
    }

    public void setLicense(List<ZenodoLicense> license) {
        this.license = license;
    }

    public List<ZenodoDates> getDates() {
        return dates;
    }

    public void setDates(List<ZenodoDates> dates) {
        this.dates = dates;
    }

    public List<ZenodoAlternateIdentifiers> getAlternateIdentifiers() {
        return alternateIdentifiers;
    }

    public void setAlternateIdentifiers(List<ZenodoAlternateIdentifiers> alternateIdentifiers) {
        this.alternateIdentifiers = alternateIdentifiers;
    }

    public List<ZenodoRelator> getRelatedIdentifiers() {
        return relatedIdentifiers;
    }

    public void setRelatedIdentifiers(List<ZenodoRelator> relatedIdentifiers) {
        this.relatedIdentifiers = relatedIdentifiers;
    }

    public List<ZenodoCreator> getContributors() {
        return contributors;
    }

    public void setContributors(List<ZenodoCreator> contributors) {
        this.contributors = contributors;
    }

    public List<ZenodoCreator> getCreators() {
        return creators;
    }

    public void setCreators(List<ZenodoCreator> creators) {
        this.creators = creators;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public ZenodoResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ZenodoResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public List<ZenodoFunding> getFunding() {
        return funding;
    }

    public void setFunding(List<ZenodoFunding> funding) {
        this.funding = funding;
    }

    public List<ZenodoSubject> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<ZenodoSubject> subjects) {
        this.subjects = subjects;
    }

    public List<ZenodoLanguage> getLanguages() {
        return languages;
    }

    public void setLanguages(List<ZenodoLanguage> languages) {
        this.languages = languages;
    }

    public List<ZenodoAdditionalTitle> getAdditionalTitles() {
        return additionalTitles;
    }

    public void setAdditionalTitles(List<ZenodoAdditionalTitle> additionalTitles) {
        this.additionalTitles = additionalTitles;
    }

    public List<ZenodoReference> getReferences() {
        return references;
    }

    public void setReferences(List<ZenodoReference> references) {
        this.references = references;
    }
}

package org.opencdmp.deposit.zenodorepository.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ZenodoDepositCustomFields {

    @JsonProperty("journal:journal")
    private Journal journal;

    @JsonProperty("imprint:imprint")
    private Imprint imprint;

    @JsonProperty("thesis:thesis")
    private Thesis thesis;

    @JsonProperty("meeting:meeting")
    private Conference conference;

    @JsonProperty("code:codeRepository")
    private String codeRepository;

    @JsonProperty("code:programmingLanguage")
    private List<Code> codeProgrammingLanguage;

    @JsonProperty("code:developmentStatus")
    private Code codeDevelopmentStatus;

    /**
     * Instance-specific custom fields not modeled above (e.g. domain fields such as
     * dwc:*), declared per deployment via preview-extra-fields configuration.
     * Serialized flat into custom_fields; unknown keys are silently dropped by Zenodo.
     */
    private final Map<String, Object> additionalProperties = new LinkedHashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String key, Object value) {
        this.additionalProperties.put(key, value);
    }

    public Journal getJournal() {
        return journal;
    }

    public void setJournal(Journal journal) {
        this.journal = journal;
    }

    public Imprint getImprint() {
        return imprint;
    }

    public void setImprint(Imprint imprint) {
        this.imprint = imprint;
    }

    public Conference getConference() {
        return conference;
    }

    public void setConference(Conference conference) {
        this.conference = conference;
    }

    public Thesis getThesis() {
        return thesis;
    }

    public void setThesis(Thesis thesis) {
        this.thesis = thesis;
    }

    public String getCodeRepository() {
        return codeRepository;
    }

    public void setCodeRepository(String codeRepository) {
        this.codeRepository = codeRepository;
    }

    public List<Code> getCodeProgrammingLanguage() {
        return codeProgrammingLanguage;
    }

    public void setCodeProgrammingLanguage(List<Code> codeProgrammingLanguage) {
        this.codeProgrammingLanguage = codeProgrammingLanguage;
    }

    public Code getCodeDevelopmentStatus() {
        return codeDevelopmentStatus;
    }

    public void setCodeDevelopmentStatus(Code codeDevelopmentStatus) {
        this.codeDevelopmentStatus = codeDevelopmentStatus;
    }

    public static class Journal {
        private String title;

        private String issn;

        private String volume;

        private String issue;

        private String pages;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getIssn() {
            return issn;
        }

        public void setIssn(String issn) {
            this.issn = issn;
        }

        public String getVolume() {
            return volume;
        }

        public void setVolume(String volume) {
            this.volume = volume;
        }

        public String getIssue() {
            return issue;
        }

        public void setIssue(String issue) {
            this.issue = issue;
        }

        public String getPages() {
            return pages;
        }

        public void setPages(String pages) {
            this.pages = pages;
        }
    }

    public static class Imprint {

        private String title;

        private String isbn;

        private String place;

        private String pages;

        private String edition;

        public String getEdition() {
            return edition;
        }

        public void setEdition(String edition) {
            this.edition = edition;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getIsbn() {
            return isbn;
        }

        public void setIsbn(String isbn) {
            this.isbn = isbn;
        }

        public String getPlace() {
            return place;
        }

        public void setPlace(String place) {
            this.place = place;
        }

        public String getPages() {
            return pages;
        }

        public void setPages(String pages) {
            this.pages = pages;
        }
    }

    public static class Conference {
        private String title;

        private String acronym;

        private String dates;

        private String place;

        private String url;

        private String session;

        @JsonProperty("session_part")
        private String sessionPart;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getAcronym() {
            return acronym;
        }

        public void setAcronym(String acronym) {
            this.acronym = acronym;
        }

        public String getDates() {
            return dates;
        }

        public void setDates(String dates) {
            this.dates = dates;
        }

        public String getPlace() {
            return place;
        }

        public void setPlace(String place) {
            this.place = place;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getSession() {
            return session;
        }

        public void setSession(String session) {
            this.session = session;
        }

        public String getSessionPart() {
            return sessionPart;
        }

        public void setSessionPart(String sessionPart) {
            this.sessionPart = sessionPart;
        }
    }

    /** The thesis:thesis nested custom field (InvenioRDM replaced the flat thesis:university with this object). */
    public static class Thesis {

        private String university;

        private String department;

        private String type;

        @JsonProperty("date_submitted")
        private String dateSubmitted;

        @JsonProperty("date_defended")
        private String dateDefended;

        public String getUniversity() {
            return university;
        }

        public void setUniversity(String university) {
            this.university = university;
        }

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDateSubmitted() {
            return dateSubmitted;
        }

        public void setDateSubmitted(String dateSubmitted) {
            this.dateSubmitted = dateSubmitted;
        }

        public String getDateDefended() {
            return dateDefended;
        }

        public void setDateDefended(String dateDefended) {
            this.dateDefended = dateDefended;
        }
    }

    public static class Code {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

}

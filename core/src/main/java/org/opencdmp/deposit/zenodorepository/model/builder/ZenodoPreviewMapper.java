package org.opencdmp.deposit.zenodorepository.model.builder;

import org.opencdmp.commonmodels.enums.PreviewFieldSource;
import org.opencdmp.commonmodels.enums.PreviewFieldType;
import org.opencdmp.commonmodels.models.deposit.DepositPreviewModel;
import org.opencdmp.commonmodels.models.deposit.PreviewField;
import org.opencdmp.commonmodels.models.deposit.PreviewFieldGroup;
import org.opencdmp.commonmodels.models.deposit.PreviewFieldOption;
import org.opencdmp.commonmodels.models.deposit.PreviewFieldValue;
import org.opencdmp.commonmodels.models.deposit.PreviewGroupItemModel;
import org.opencdmp.commonmodels.models.deposit.PreviewGroupValuesModel;
import org.opencdmp.deposit.zenodorepository.configuration.preview.PreviewExtraFieldsProperties;
import org.opencdmp.deposit.zenodorepository.enums.ZenodoAccessRight;
import org.opencdmp.deposit.zenodorepository.enums.ZenodoAdditionalTitleIdType;
import org.opencdmp.deposit.zenodorepository.model.ZenodoAdditionalDescription;
import org.opencdmp.deposit.zenodorepository.model.ZenodoAdditionalTitle;
import org.opencdmp.deposit.zenodorepository.model.ZenodoAlternateIdentifiers;
import org.opencdmp.deposit.zenodorepository.model.ZenodoCreator;
import org.opencdmp.deposit.zenodorepository.model.ZenodoDates;
import org.opencdmp.deposit.zenodorepository.model.ZenodoDeposit;
import org.opencdmp.deposit.zenodorepository.model.ZenodoDepositAccess;
import org.opencdmp.deposit.zenodorepository.model.ZenodoDepositCustomFields;
import org.opencdmp.deposit.zenodorepository.model.ZenodoFunding;
import org.opencdmp.deposit.zenodorepository.model.ZenodoLanguage;
import org.opencdmp.deposit.zenodorepository.model.ZenodoLicense;
import org.opencdmp.deposit.zenodorepository.model.ZenodoReference;
import org.opencdmp.deposit.zenodorepository.model.ZenodoRelator;
import org.opencdmp.deposit.zenodorepository.model.ZenodoSubject;
import org.opencdmp.depositbase.repository.DepositPluginSettings;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Maps a built {@link ZenodoDeposit} to a deposit preview descriptor and applies
 * user-edited preview values back onto a built deposit before submission. Both
 * directions live in this class so the preview form and the deposited metadata
 * always derive from the same mapping.
 */
@Component
public class ZenodoPreviewMapper {

    private final PreviewExtraFieldsProperties previewExtraFieldsProperties;

    public ZenodoPreviewMapper(PreviewExtraFieldsProperties previewExtraFieldsProperties) {
        this.previewExtraFieldsProperties = previewExtraFieldsProperties;
    }

    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_PUBLICATION_DATE = "publication-date";
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_PUBLISHER = "publisher";
    public static final String FIELD_RESOURCE_TYPE = "resource-type";
    public static final String FIELD_LANGUAGE = "language";
    public static final String FIELD_KEYWORDS = "keywords";
    public static final String FIELD_LICENSE = "license";
    public static final String FIELD_ACCESS_FILES = "access-files";
    public static final String FIELD_EMBARGO_UNTIL = "embargo-until";
    public static final String FIELD_EMBARGO_REASON = "embargo-reason";
    public static final String FIELD_ADDITIONAL_DESCRIPTION = "additional-description";
    public static final String FIELD_REFERENCES = "references";
    public static final String FIELD_COMMUNITY = "community";
    public static final String FIELD_ADDITIONAL_COMMUNITIES = "additional-communities";
    public static final String FIELD_FILES = "files";
    public static final String FIELD_COPYRIGHT = "copyright";
    public static final String FIELD_RELATED_WORKS = "related-works";
    public static final String FIELD_JOURNAL_TITLE = "journal-title";
    public static final String FIELD_JOURNAL_ISSN = "journal-issn";
    public static final String FIELD_JOURNAL_VOLUME = "journal-volume";
    public static final String FIELD_JOURNAL_ISSUE = "journal-issue";
    public static final String FIELD_JOURNAL_PAGES = "journal-pages";
    public static final String FIELD_IMPRINT_TITLE = "imprint-title";
    public static final String FIELD_IMPRINT_ISBN = "imprint-isbn";
    public static final String FIELD_IMPRINT_PLACE = "imprint-place";
    public static final String FIELD_IMPRINT_PAGES = "imprint-pages";
    public static final String FIELD_IMPRINT_EDITION = "imprint-edition";
    public static final String FIELD_THESIS_UNIVERSITY = "thesis-university";
    public static final String FIELD_THESIS_DEPARTMENT = "thesis-department";
    public static final String FIELD_THESIS_TYPE = "thesis-type";
    public static final String FIELD_THESIS_DATE_SUBMITTED = "thesis-date-submitted";
    public static final String FIELD_THESIS_DATE_DEFENDED = "thesis-date-defended";
    public static final String FIELD_CONFERENCE_TITLE = "conference-title";
    public static final String FIELD_CONFERENCE_ACRONYM = "conference-acronym";
    public static final String FIELD_CONFERENCE_PLACE = "conference-place";
    public static final String FIELD_CONFERENCE_DATES = "conference-dates";
    public static final String FIELD_CONFERENCE_SESSION = "conference-session";
    public static final String FIELD_CONFERENCE_PART = "conference-part";
    public static final String FIELD_CONFERENCE_WEBSITE = "conference-website";
    public static final String FIELD_SOFTWARE_REPOSITORY_URL = "software-repository-url";
    public static final String FIELD_SOFTWARE_PROGRAMMING_LANGUAGE = "software-programming-language";
    public static final String FIELD_SOFTWARE_DEVELOPMENT_STATUS = "software-development-status";

    public static final String GROUP_CREATORS = "creators";
    public static final String GROUP_CONTRIBUTORS = "contributors";
    public static final String GROUP_FUNDING = "funding";
    public static final String GROUP_ADDITIONAL_TITLES = "additional-titles";
    public static final String GROUP_ADDITIONAL_DESCRIPTIONS = "additional-descriptions";
    public static final String GROUP_DATES = "dates";
    public static final String GROUP_ALTERNATE_IDENTIFIERS = "alternate-identifiers";
    public static final String GROUP_FILES_CODE = "files";
    public static final String GROUP_DOMAIN_SPECIFIC_CODE = "domain-specific";

    public static final String ITEM_PERSON_TYPE = "person-type";
    public static final String ITEM_GIVEN_NAME = "given-name";
    public static final String ITEM_FAMILY_NAME = "family-name";
    public static final String ITEM_NAME = "name";
    public static final String ITEM_ORCID = "orcid";
    public static final String ITEM_AFFILIATION = "affiliation";
    public static final String ITEM_ROLE = "role";
    public static final String ITEM_FUNDER_NAME = "funder-name";
    public static final String ITEM_FUNDER_ID = "funder-id";
    public static final String ITEM_AWARD_TITLE = "award-title";
    public static final String ITEM_AWARD_NUMBER = "award-number";
    public static final String ITEM_ADDITIONAL_TITLE = "additional-title";
    public static final String ITEM_ADDITIONAL_TITLE_TYPE = "additional-title-type";
    public static final String ITEM_ADDITIONAL_TITLE_LANG = "additional-title-lang";
    public static final String ITEM_DATE_VALUE = "date-value";
    public static final String ITEM_DATE_TYPE = "date-type";
    public static final String ITEM_DATE_DESCRIPTION = "date-description";
    public static final String ITEM_IDENTIFIER_VALUE = "identifier-value";
    public static final String ITEM_IDENTIFIER_SCHEME = "identifier-scheme";
    public static final String ITEM_ADDITIONAL_DESCRIPTION_TEXT = "additional-description-text";
    public static final String ITEM_ADDITIONAL_DESCRIPTION_TYPE = "additional-description-type";

    public static final String WIDGET_PERSON = "person";
    public static final String WIDGET_FUNDING = "funding";
    public static final String WIDGET_PID = "pid";
    public static final String WIDGET_RICHTEXT = "richtext";
    /** On a multi-value vocabulary field: free-text entries are allowed alongside vocabulary picks. */
    public static final String WIDGET_ALLOW_CUSTOM = "allow-custom";

    // vocabulary license ids are lowercase slugs; anything else submitted on the license field is a custom license title
    private static final java.util.regex.Pattern LICENSE_ID_PATTERN = java.util.regex.Pattern.compile("^[a-z0-9][a-z0-9.+-]*$");

    private static final String ADDITIONAL_DESCRIPTION_TYPE_OTHER = "other";
    private static final String ORCID_SCHEME = "orcid";
    private static final String PERSON_TYPE_PERSONAL = "personal";
    private static final String PERSON_TYPE_ORGANIZATIONAL = "organizational";

    // preview field code -> the record-default setting an organisation can lock it with
    private static final Map<String, String> LOCKABLE_FIELDS = Map.of(
            FIELD_COMMUNITY, "community",
            FIELD_PUBLISHER, "publisher",
            FIELD_RESOURCE_TYPE, "resource-type",
            FIELD_LICENSE, "default-licence",
            FIELD_LANGUAGE, "default-language",
            FIELD_COPYRIGHT, "copyright");
    private static final String SETTING_ADDITIONAL_COMMUNITIES = "additional-communities";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private static final List<PreviewFieldOption> LICENSE_OPTIONS = List.of(
            new PreviewFieldOption("Creative Commons Attribution 4.0", "cc-by-4.0"),
            new PreviewFieldOption("Creative Commons Attribution Share Alike 4.0", "cc-by-sa-4.0"),
            new PreviewFieldOption("Creative Commons Attribution Non Commercial 4.0", "cc-by-nc-4.0"),
            new PreviewFieldOption("Creative Commons Zero 1.0", "cc0-1.0"),
            new PreviewFieldOption("MIT License", "mit"),
            new PreviewFieldOption("Apache License 2.0", "apache-2.0"));

    private static final List<PreviewFieldOption> LANGUAGE_OPTIONS = List.of(
            new PreviewFieldOption("English", "eng"),
            new PreviewFieldOption("German", "deu"),
            new PreviewFieldOption("French", "fra"),
            new PreviewFieldOption("Greek", "ell"),
            new PreviewFieldOption("Spanish", "spa"),
            new PreviewFieldOption("Italian", "ita"));

    private static final List<PreviewFieldOption> ACCESS_OPTIONS = List.of(
            new PreviewFieldOption("Public", ZenodoAccessRight.Names.Public),
            new PreviewFieldOption("Restricted", ZenodoAccessRight.Names.Restricted));

    private static final List<PreviewFieldOption> PERSON_TYPE_OPTIONS = List.of(
            new PreviewFieldOption("Person", PERSON_TYPE_PERSONAL),
            new PreviewFieldOption("Organization", PERSON_TYPE_ORGANIZATIONAL));

    private static final List<PreviewFieldOption> DESCRIPTION_TYPE_OPTIONS = List.of(
            new PreviewFieldOption("Abstract", "abstract"),
            new PreviewFieldOption("Methods", "methods"),
            new PreviewFieldOption("Series information", "series-information"),
            new PreviewFieldOption("Table of contents", "table-of-contents"),
            new PreviewFieldOption("Technical info", "technical-info"),
            new PreviewFieldOption("Other", ADDITIONAL_DESCRIPTION_TYPE_OTHER));

    private static final List<PreviewFieldOption> TITLE_TYPE_OPTIONS = List.of(
            new PreviewFieldOption("Alternative title", ZenodoAdditionalTitleIdType.Names.AlternativeTitle),
            new PreviewFieldOption("Subtitle", ZenodoAdditionalTitleIdType.Names.Subtitle),
            new PreviewFieldOption("Translated title", ZenodoAdditionalTitleIdType.Names.TranslatedTitle),
            new PreviewFieldOption("Other", ZenodoAdditionalTitleIdType.Names.Other));

    private static final List<PreviewFieldOption> DATE_TYPE_OPTIONS = List.of(
            new PreviewFieldOption("Accepted", "accepted"),
            new PreviewFieldOption("Available", "available"),
            new PreviewFieldOption("Collected", "collected"),
            new PreviewFieldOption("Copyrighted", "copyrighted"),
            new PreviewFieldOption("Created", "created"),
            new PreviewFieldOption("Issued", "issued"),
            new PreviewFieldOption("Other", "other"),
            new PreviewFieldOption("Submitted", "submitted"),
            new PreviewFieldOption("Updated", "updated"),
            new PreviewFieldOption("Valid", "valid"),
            new PreviewFieldOption("Withdrawn", "withdrawn"));

    // mirrors the schemes accepted by the semantics configuration (semantics.yml alternate-identifiers-scheme)
    private static final List<PreviewFieldOption> IDENTIFIER_SCHEME_OPTIONS = List.of(
            new PreviewFieldOption("ARK", "ark"),
            new PreviewFieldOption("arXiv", "arxiv"),
            new PreviewFieldOption("ADS", "ads"),
            new PreviewFieldOption("Crossref Funder ID", "crossreffunderid"),
            new PreviewFieldOption("DOI", "doi"),
            new PreviewFieldOption("EAN13", "ean13"),
            new PreviewFieldOption("eISSN", "eissn"),
            new PreviewFieldOption("GRID", "grid"),
            new PreviewFieldOption("Handle", "handle"),
            new PreviewFieldOption("IGSN", "igsn"),
            new PreviewFieldOption("ISBN", "isbn"),
            new PreviewFieldOption("ISNI", "isni"),
            new PreviewFieldOption("ISSN", "issn"),
            new PreviewFieldOption("ISTC", "istc"),
            new PreviewFieldOption("LISSN", "lissn"),
            new PreviewFieldOption("LSID", "lsid"),
            new PreviewFieldOption("PMID", "pmid"),
            new PreviewFieldOption("PURL", "purl"),
            new PreviewFieldOption("UPC", "upc"),
            new PreviewFieldOption("URL", "url"),
            new PreviewFieldOption("URN", "urn"),
            new PreviewFieldOption("W3ID", "w3id"),
            new PreviewFieldOption("Other", "other"));

    // mirrors programming-languages.yml development-status
    private static final List<PreviewFieldOption> DEVELOPMENT_STATUS_OPTIONS = List.of(
            new PreviewFieldOption("Concept", "concept"),
            new PreviewFieldOption("Work in progress", "wip"),
            new PreviewFieldOption("Active", "active"),
            new PreviewFieldOption("Inactive", "inactive"),
            new PreviewFieldOption("Unsupported", "unsupported"),
            new PreviewFieldOption("Moved", "moved"),
            new PreviewFieldOption("Suspended", "suspended"),
            new PreviewFieldOption("Abandoned", "abandoned"));

    private static final List<PreviewFieldOption> ROLE_OPTIONS = List.of(
            new PreviewFieldOption("Researcher", "researcher"),
            new PreviewFieldOption("Project member", "projectmember"),
            new PreviewFieldOption("Project manager", "projectmanager"),
            new PreviewFieldOption("Data manager", "datamanager"),
            new PreviewFieldOption("Contact person", "contactperson"),
            new PreviewFieldOption("Editor", "editor"),
            new PreviewFieldOption("Other", "other"));

    public DepositPreviewModel toPreview(ZenodoDeposit deposit, String community, List<String> uploadFileNames, DepositPluginSettings settings) {
        DepositPreviewModel model = new DepositPreviewModel();
        List<PreviewFieldGroup> groups = new ArrayList<>();
        if (uploadFileNames != null && !uploadFileNames.isEmpty()) groups.add(this.buildFilesGroup(uploadFileNames));
        groups.add(this.buildBasicInformationGroup(deposit));
        groups.add(this.buildLicenseAndAccessGroup(deposit));
        groups.add(this.buildCreatorsGroup(GROUP_CREATORS, "Creators", deposit.getMetadata() != null ? deposit.getMetadata().getCreators() : null, false));
        groups.add(this.buildCreatorsGroup(GROUP_CONTRIBUTORS, "Contributors", deposit.getMetadata() != null ? deposit.getMetadata().getContributors() : null, true));
        groups.add(this.buildFundingGroup(deposit));
        groups.add(this.buildAdditionalTitlesGroup(deposit));
        groups.add(this.buildDatesGroup(deposit));
        groups.add(this.buildAlternateIdentifiersGroup(deposit));
        groups.add(this.buildPublishingInformationGroup(deposit));
        groups.add(this.buildConferenceGroup(deposit));
        groups.add(this.buildSoftwareGroup(deposit));
        groups.add(this.buildAdditionalDescriptionsGroup(deposit));
        if (this.previewExtraFieldsProperties.getFields() != null && !this.previewExtraFieldsProperties.getFields().isEmpty()) groups.add(this.buildDomainSpecificGroup(deposit));
        groups.add(this.buildAdditionalInformationGroup(deposit, community, settings.list(SETTING_ADDITIONAL_COMMUNITIES)));
        groups.removeIf(group -> settings.isHidden(group.getCode()));
        for (PreviewFieldGroup group : groups) {
            for (PreviewField field : group.getFields()) {
                if (this.isLocked(field.getCode(), settings)) field.setSource(PreviewFieldSource.System);
            }
        }
        model.setGroups(groups);
        return model;
    }

    private PreviewFieldGroup buildBasicInformationGroup(ZenodoDeposit deposit) {
        PreviewFieldGroup group = this.buildGroup("basic-information", "Basic information");
        List<PreviewField> fields = group.getFields();

        fields.add(this.buildField(FIELD_TITLE, "Title", PreviewFieldType.Text, PreviewFieldSource.Mapped, true,
                () -> this.textValue(FIELD_TITLE, deposit.getMetadata() != null ? deposit.getMetadata().getTitle() : null)));
        PreviewField description = this.buildField(FIELD_DESCRIPTION, "Description", PreviewFieldType.TextArea, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_DESCRIPTION, deposit.getMetadata() != null ? deposit.getMetadata().getDescription() : null));
        description.setWidgetHint(WIDGET_RICHTEXT);
        fields.add(description);
        fields.add(this.buildField(FIELD_PUBLICATION_DATE, "Publication date", PreviewFieldType.Date, PreviewFieldSource.Mapped, true,
                () -> this.dateValue(FIELD_PUBLICATION_DATE, deposit.getMetadata() != null ? deposit.getMetadata().getPublicationDate() : null)));
        fields.add(this.buildField(FIELD_VERSION, "Version", PreviewFieldType.Text, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_VERSION, deposit.getMetadata() != null ? deposit.getMetadata().getVersion() : null)));
        fields.add(this.buildField(FIELD_PUBLISHER, "Publisher", PreviewFieldType.Text, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_PUBLISHER, deposit.getMetadata() != null ? deposit.getMetadata().getPublisher() : null)));

        PreviewField language = this.buildField(FIELD_LANGUAGE, "Languages", PreviewFieldType.Select, PreviewFieldSource.Mapped, false,
                () -> this.textListValue(FIELD_LANGUAGE, this.languageIds(deposit)));
        language.setOptions(LANGUAGE_OPTIONS);
        language.setVocabSearch(true);
        language.setMultipleSelect(true);
        fields.add(language);

        PreviewField keywords = this.buildField(FIELD_KEYWORDS, "Keywords and subjects", PreviewFieldType.Tags, PreviewFieldSource.Mapped, false,
                () -> this.textListValue(FIELD_KEYWORDS, this.subjectLabels(deposit)));
        keywords.setVocabSearch(true);
        fields.add(keywords);
        fields.add(this.buildField(FIELD_RESOURCE_TYPE, "Resource type", PreviewFieldType.ReadOnly, PreviewFieldSource.System, false,
                () -> this.textValue(FIELD_RESOURCE_TYPE, deposit.getMetadata() != null && deposit.getMetadata().getResourceType() != null ? deposit.getMetadata().getResourceType().getId() : null)));

        return group;
    }

    private PreviewFieldGroup buildLicenseAndAccessGroup(ZenodoDeposit deposit) {
        PreviewFieldGroup group = this.buildGroup("license-access", "License and access");
        List<PreviewField> fields = group.getFields();

        PreviewField license = this.buildField(FIELD_LICENSE, "Licenses", PreviewFieldType.Select, PreviewFieldSource.Mapped, false,
                () -> this.textListValue(FIELD_LICENSE, this.licenseIds(deposit)));
        license.setOptions(LICENSE_OPTIONS);
        license.setVocabSearch(true);
        license.setMultipleSelect(true);
        license.setWidgetHint(WIDGET_ALLOW_CUSTOM);
        fields.add(license);

        ZenodoDepositAccess access = deposit.getAccess();
        PreviewField accessFiles = this.buildField(FIELD_ACCESS_FILES, "File access", PreviewFieldType.Select, PreviewFieldSource.Mapped, true,
                () -> this.textValue(FIELD_ACCESS_FILES, access != null && access.getFiles() != null ? access.getFiles().getValue() : null));
        accessFiles.setOptions(ACCESS_OPTIONS);
        fields.add(accessFiles);

        ZenodoDepositAccess.Embargo embargo = access != null ? access.getEmbargo() : null;
        fields.add(this.buildField(FIELD_EMBARGO_UNTIL, "Embargo until", PreviewFieldType.Date, PreviewFieldSource.Mapped, false,
                () -> this.dateValue(FIELD_EMBARGO_UNTIL, embargo != null && embargo.isActive() ? embargo.getUntil() : null)));
        fields.add(this.buildField(FIELD_EMBARGO_REASON, "Embargo reason", PreviewFieldType.TextArea, PreviewFieldSource.Manual, false,
                () -> this.textValue(FIELD_EMBARGO_REASON, embargo != null ? embargo.getReason() : null)));
        fields.add(this.buildField(FIELD_COPYRIGHT, "Copyright", PreviewFieldType.TextArea, PreviewFieldSource.Manual, false,
                () -> this.textValue(FIELD_COPYRIGHT, deposit.getMetadata() != null ? deposit.getMetadata().getCopyright() : null)));

        return group;
    }

    private PreviewFieldGroup buildCreatorsGroup(String code, String label, List<ZenodoCreator> creators, boolean withRole) {
        PreviewFieldGroup group = this.buildGroup(code, label);
        group.setRepeatable(true);
        group.setWidgetHint(WIDGET_PERSON);
        List<PreviewField> fields = group.getFields();

        PreviewField personType = this.buildTemplateField(ITEM_PERSON_TYPE, "Type", PreviewFieldType.Select);
        personType.setOptions(PERSON_TYPE_OPTIONS);
        fields.add(personType);
        fields.add(this.buildTemplateField(ITEM_GIVEN_NAME, "Given name", PreviewFieldType.Text));
        fields.add(this.buildTemplateField(ITEM_FAMILY_NAME, "Family name", PreviewFieldType.Text));
        fields.add(this.buildTemplateField(ITEM_NAME, "Name (organization or full name)", PreviewFieldType.Text));
        PreviewField orcid = this.buildTemplateField(ITEM_ORCID, "ORCID", PreviewFieldType.Text);
        orcid.setWidgetHint(WIDGET_PID);
        fields.add(orcid);
        fields.add(this.buildTemplateField(ITEM_AFFILIATION, "Affiliation", PreviewFieldType.Text));
        if (withRole) {
            PreviewField role = this.buildTemplateField(ITEM_ROLE, "Role", PreviewFieldType.Select);
            role.setOptions(ROLE_OPTIONS);
            fields.add(role);
        }

        List<PreviewGroupItemModel> items = new ArrayList<>();
        if (creators != null) {
            for (ZenodoCreator creator : creators) {
                if (creator == null || creator.getPersonOrOrg() == null) continue;
                ZenodoCreator.PersonOrOrg person = creator.getPersonOrOrg();
                List<PreviewFieldValue> values = new ArrayList<>();
                values.add(this.textValue(ITEM_PERSON_TYPE, person.getType()));
                values.add(this.textValue(ITEM_GIVEN_NAME, person.getGivenName()));
                values.add(this.textValue(ITEM_FAMILY_NAME, person.getFamilyName()));
                values.add(this.textValue(ITEM_NAME, person.getName()));
                values.add(this.textValue(ITEM_ORCID, this.findIdentifier(person, ORCID_SCHEME)));
                values.add(this.textValue(ITEM_AFFILIATION, creator.getAffiliations() != null && !creator.getAffiliations().isEmpty() && creator.getAffiliations().getFirst() != null ? creator.getAffiliations().getFirst().getName() : null));
                if (withRole) values.add(this.textValue(ITEM_ROLE, creator.getRole() != null ? creator.getRole().getId() : null));
                PreviewGroupItemModel item = new PreviewGroupItemModel();
                item.setValues(values);
                items.add(item);
            }
        }
        group.setItems(items);
        group.setBaseItems(items);
        return group;
    }

    private PreviewFieldGroup buildFundingGroup(ZenodoDeposit deposit) {
        PreviewFieldGroup group = this.buildGroup(GROUP_FUNDING, "Funding");
        group.setRepeatable(true);
        group.setWidgetHint(WIDGET_FUNDING);
        List<PreviewField> fields = group.getFields();

        PreviewField funderName = this.buildTemplateField(ITEM_FUNDER_NAME, "Funder", PreviewFieldType.Text);
        funderName.setVocabSearch(true);
        fields.add(funderName);
        PreviewField funderId = this.buildTemplateField(ITEM_FUNDER_ID, "Funder identifier (ROR / DOI)", PreviewFieldType.Text);
        funderId.setWidgetHint(WIDGET_PID);
        fields.add(funderId);
        PreviewField awardTitle = this.buildTemplateField(ITEM_AWARD_TITLE, "Award title", PreviewFieldType.Text);
        awardTitle.setVocabSearch(true);
        fields.add(awardTitle);
        PreviewField awardNumber = this.buildTemplateField(ITEM_AWARD_NUMBER, "Award number", PreviewFieldType.Text);
        awardNumber.setVocabSearch(true);
        fields.add(awardNumber);

        List<PreviewGroupItemModel> items = new ArrayList<>();
        List<ZenodoFunding> fundingList = deposit.getMetadata() != null ? deposit.getMetadata().getFunding() : null;
        if (fundingList != null) {
            for (ZenodoFunding funding : fundingList) {
                if (funding == null) continue;
                List<PreviewFieldValue> values = new ArrayList<>();
                values.add(this.textValue(ITEM_FUNDER_NAME, funding.getFunder() != null ? funding.getFunder().getName() : null));
                values.add(this.textValue(ITEM_FUNDER_ID, funding.getFunder() != null ? funding.getFunder().getId() : null));
                values.add(this.textValue(ITEM_AWARD_TITLE, funding.getGrant() != null ? funding.getGrant().getTitle() : null));
                values.add(this.textValue(ITEM_AWARD_NUMBER, funding.getGrant() != null ? funding.getGrant().getNumber() : null));
                PreviewGroupItemModel item = new PreviewGroupItemModel();
                item.setValues(values);
                items.add(item);
            }
        }
        group.setItems(items);
        group.setBaseItems(items);
        return group;
    }

    private PreviewFieldGroup buildAdditionalTitlesGroup(ZenodoDeposit deposit) {
        PreviewFieldGroup group = this.buildGroup(GROUP_ADDITIONAL_TITLES, "Additional titles");
        group.setRepeatable(true);
        List<PreviewField> fields = group.getFields();

        fields.add(this.buildTemplateField(ITEM_ADDITIONAL_TITLE, "Title", PreviewFieldType.Text));
        PreviewField titleType = this.buildTemplateField(ITEM_ADDITIONAL_TITLE_TYPE, "Type", PreviewFieldType.Select);
        titleType.setOptions(TITLE_TYPE_OPTIONS);
        fields.add(titleType);
        fields.add(this.buildTemplateField(ITEM_ADDITIONAL_TITLE_LANG, "Language (ISO 639-3)", PreviewFieldType.Text));

        List<PreviewGroupItemModel> items = new ArrayList<>();
        List<ZenodoAdditionalTitle> additionalTitles = deposit.getMetadata() != null ? deposit.getMetadata().getAdditionalTitles() : null;
        if (additionalTitles != null) {
            for (ZenodoAdditionalTitle additionalTitle : additionalTitles) {
                if (additionalTitle == null) continue;
                List<PreviewFieldValue> values = new ArrayList<>();
                values.add(this.textValue(ITEM_ADDITIONAL_TITLE, additionalTitle.getTitle()));
                values.add(this.textValue(ITEM_ADDITIONAL_TITLE_TYPE, additionalTitle.getType() != null && additionalTitle.getType().getId() != null ? additionalTitle.getType().getId().getValue() : null));
                values.add(this.textValue(ITEM_ADDITIONAL_TITLE_LANG, additionalTitle.getLang() != null ? additionalTitle.getLang().getId() : null));
                PreviewGroupItemModel item = new PreviewGroupItemModel();
                item.setValues(values);
                items.add(item);
            }
        }
        group.setItems(items);
        group.setBaseItems(items);
        return group;
    }

    private PreviewFieldGroup buildDatesGroup(ZenodoDeposit deposit) {
        PreviewFieldGroup group = this.buildGroup(GROUP_DATES, "Dates");
        group.setRepeatable(true);
        List<PreviewField> fields = group.getFields();

        fields.add(this.buildTemplateField(ITEM_DATE_VALUE, "Date (YYYY-MM-DD or interval)", PreviewFieldType.Text));
        PreviewField dateType = this.buildTemplateField(ITEM_DATE_TYPE, "Type", PreviewFieldType.Select);
        dateType.setOptions(DATE_TYPE_OPTIONS);
        fields.add(dateType);
        fields.add(this.buildTemplateField(ITEM_DATE_DESCRIPTION, "Description", PreviewFieldType.Text));

        List<PreviewGroupItemModel> items = new ArrayList<>();
        List<ZenodoDates> dates = deposit.getMetadata() != null ? deposit.getMetadata().getDates() : null;
        if (dates != null) {
            for (ZenodoDates date : dates) {
                if (date == null) continue;
                List<PreviewFieldValue> values = new ArrayList<>();
                values.add(this.textValue(ITEM_DATE_VALUE, date.getDate()));
                values.add(this.textValue(ITEM_DATE_TYPE, date.getType() != null ? date.getType().getId() : null));
                values.add(this.textValue(ITEM_DATE_DESCRIPTION, date.getDescription()));
                PreviewGroupItemModel item = new PreviewGroupItemModel();
                item.setValues(values);
                items.add(item);
            }
        }
        group.setItems(items);
        group.setBaseItems(items);
        return group;
    }

    private PreviewFieldGroup buildAlternateIdentifiersGroup(ZenodoDeposit deposit) {
        PreviewFieldGroup group = this.buildGroup(GROUP_ALTERNATE_IDENTIFIERS, "Alternate identifiers");
        group.setRepeatable(true);
        List<PreviewField> fields = group.getFields();

        PreviewField identifier = this.buildTemplateField(ITEM_IDENTIFIER_VALUE, "Identifier", PreviewFieldType.Text);
        identifier.setWidgetHint(WIDGET_PID);
        fields.add(identifier);
        PreviewField scheme = this.buildTemplateField(ITEM_IDENTIFIER_SCHEME, "Scheme", PreviewFieldType.Select);
        scheme.setOptions(IDENTIFIER_SCHEME_OPTIONS);
        fields.add(scheme);

        List<PreviewGroupItemModel> items = new ArrayList<>();
        List<ZenodoAlternateIdentifiers> alternateIdentifiers = deposit.getMetadata() != null ? deposit.getMetadata().getAlternateIdentifiers() : null;
        if (alternateIdentifiers != null) {
            for (ZenodoAlternateIdentifiers alternateIdentifier : alternateIdentifiers) {
                if (alternateIdentifier == null) continue;
                List<PreviewFieldValue> values = new ArrayList<>();
                values.add(this.textValue(ITEM_IDENTIFIER_VALUE, alternateIdentifier.getIdentifier()));
                values.add(this.textValue(ITEM_IDENTIFIER_SCHEME, alternateIdentifier.getScheme()));
                PreviewGroupItemModel item = new PreviewGroupItemModel();
                item.setValues(values);
                items.add(item);
            }
        }
        group.setItems(items);
        group.setBaseItems(items);
        return group;
    }

    private PreviewFieldGroup buildPublishingInformationGroup(ZenodoDeposit deposit) {
        PreviewFieldGroup group = this.buildGroup("publishing-information", "Publishing information");
        List<PreviewField> fields = group.getFields();
        ZenodoDepositCustomFields customFields = deposit.getCustomFields();
        ZenodoDepositCustomFields.Journal journal = customFields != null ? customFields.getJournal() : null;
        ZenodoDepositCustomFields.Imprint imprint = customFields != null ? customFields.getImprint() : null;
        ZenodoDepositCustomFields.Thesis thesis = customFields != null ? customFields.getThesis() : null;

        fields.add(this.buildField(FIELD_JOURNAL_TITLE, "Journal title", PreviewFieldType.Text, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_JOURNAL_TITLE, journal != null ? journal.getTitle() : null)));
        fields.add(this.buildField(FIELD_JOURNAL_ISSN, "Journal ISSN", PreviewFieldType.Text, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_JOURNAL_ISSN, journal != null ? journal.getIssn() : null)));
        fields.add(this.buildField(FIELD_JOURNAL_VOLUME, "Journal volume", PreviewFieldType.Text, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_JOURNAL_VOLUME, journal != null ? journal.getVolume() : null)));
        fields.add(this.buildField(FIELD_JOURNAL_ISSUE, "Journal issue", PreviewFieldType.Text, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_JOURNAL_ISSUE, journal != null ? journal.getIssue() : null)));
        fields.add(this.buildField(FIELD_JOURNAL_PAGES, "Journal page range or article number", PreviewFieldType.Text, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_JOURNAL_PAGES, journal != null ? journal.getPages() : null)));

        fields.add(this.buildField(FIELD_IMPRINT_TITLE, "Book, report, or proceedings title", PreviewFieldType.Text, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_IMPRINT_TITLE, imprint != null ? imprint.getTitle() : null)));
        fields.add(this.buildField(FIELD_IMPRINT_ISBN, "ISBN", PreviewFieldType.Text, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_IMPRINT_ISBN, imprint != null ? imprint.getIsbn() : null)));
        fields.add(this.buildField(FIELD_IMPRINT_PLACE, "Publication place", PreviewFieldType.Text, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_IMPRINT_PLACE, imprint != null ? imprint.getPlace() : null)));
        fields.add(this.buildField(FIELD_IMPRINT_PAGES, "Pagination", PreviewFieldType.Text, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_IMPRINT_PAGES, imprint != null ? imprint.getPages() : null)));
        fields.add(this.buildField(FIELD_IMPRINT_EDITION, "Edition", PreviewFieldType.Text, PreviewFieldSource.Manual, false,
                () -> this.textValue(FIELD_IMPRINT_EDITION, imprint != null ? imprint.getEdition() : null)));

        fields.add(this.buildField(FIELD_THESIS_UNIVERSITY, "Awarding university", PreviewFieldType.Text, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_THESIS_UNIVERSITY, thesis != null ? thesis.getUniversity() : null)));
        fields.add(this.buildField(FIELD_THESIS_DEPARTMENT, "Awarding department", PreviewFieldType.Text, PreviewFieldSource.Manual, false,
                () -> this.textValue(FIELD_THESIS_DEPARTMENT, thesis != null ? thesis.getDepartment() : null)));
        fields.add(this.buildField(FIELD_THESIS_TYPE, "Thesis type", PreviewFieldType.Text, PreviewFieldSource.Manual, false,
                () -> this.textValue(FIELD_THESIS_TYPE, thesis != null ? thesis.getType() : null)));
        fields.add(this.buildField(FIELD_THESIS_DATE_SUBMITTED, "Thesis submission date (YYYY-MM-DD)", PreviewFieldType.Text, PreviewFieldSource.Manual, false,
                () -> this.textValue(FIELD_THESIS_DATE_SUBMITTED, thesis != null ? thesis.getDateSubmitted() : null)));
        fields.add(this.buildField(FIELD_THESIS_DATE_DEFENDED, "Thesis defense date (YYYY-MM-DD)", PreviewFieldType.Text, PreviewFieldSource.Manual, false,
                () -> this.textValue(FIELD_THESIS_DATE_DEFENDED, thesis != null ? thesis.getDateDefended() : null)));

        return group;
    }

    private PreviewFieldGroup buildConferenceGroup(ZenodoDeposit deposit) {
        PreviewFieldGroup group = this.buildGroup("conference", "Conference");
        List<PreviewField> fields = group.getFields();
        ZenodoDepositCustomFields.Conference conference = deposit.getCustomFields() != null ? deposit.getCustomFields().getConference() : null;

        fields.add(this.buildField(FIELD_CONFERENCE_TITLE, "Conference title", PreviewFieldType.Text, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_CONFERENCE_TITLE, conference != null ? conference.getTitle() : null)));
        fields.add(this.buildField(FIELD_CONFERENCE_ACRONYM, "Acronym", PreviewFieldType.Text, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_CONFERENCE_ACRONYM, conference != null ? conference.getAcronym() : null)));
        fields.add(this.buildField(FIELD_CONFERENCE_PLACE, "Place", PreviewFieldType.Text, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_CONFERENCE_PLACE, conference != null ? conference.getPlace() : null)));
        fields.add(this.buildField(FIELD_CONFERENCE_DATES, "Dates", PreviewFieldType.Text, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_CONFERENCE_DATES, conference != null ? conference.getDates() : null)));
        fields.add(this.buildField(FIELD_CONFERENCE_SESSION, "Session", PreviewFieldType.Text, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_CONFERENCE_SESSION, conference != null ? conference.getSession() : null)));
        fields.add(this.buildField(FIELD_CONFERENCE_PART, "Part within the session", PreviewFieldType.Text, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_CONFERENCE_PART, conference != null ? conference.getSessionPart() : null)));
        fields.add(this.buildField(FIELD_CONFERENCE_WEBSITE, "Website", PreviewFieldType.Text, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_CONFERENCE_WEBSITE, conference != null ? conference.getUrl() : null)));

        return group;
    }

    private PreviewFieldGroup buildSoftwareGroup(ZenodoDeposit deposit) {
        PreviewFieldGroup group = this.buildGroup("software", "Software");
        List<PreviewField> fields = group.getFields();
        ZenodoDepositCustomFields customFields = deposit.getCustomFields();

        fields.add(this.buildField(FIELD_SOFTWARE_REPOSITORY_URL, "Repository URL", PreviewFieldType.Text, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_SOFTWARE_REPOSITORY_URL, customFields != null ? customFields.getCodeRepository() : null)));

        PreviewField programmingLanguage = this.buildField(FIELD_SOFTWARE_PROGRAMMING_LANGUAGE, "Programming language", PreviewFieldType.Select, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_SOFTWARE_PROGRAMMING_LANGUAGE, this.firstProgrammingLanguageId(customFields)));
        programmingLanguage.setVocabSearch(true);
        fields.add(programmingLanguage);

        PreviewField developmentStatus = this.buildField(FIELD_SOFTWARE_DEVELOPMENT_STATUS, "Development status", PreviewFieldType.Select, PreviewFieldSource.Mapped, false,
                () -> this.textValue(FIELD_SOFTWARE_DEVELOPMENT_STATUS, customFields != null && customFields.getCodeDevelopmentStatus() != null ? customFields.getCodeDevelopmentStatus().getId() : null));
        developmentStatus.setOptions(DEVELOPMENT_STATUS_OPTIONS);
        fields.add(developmentStatus);

        return group;
    }

    private PreviewFieldGroup buildAdditionalDescriptionsGroup(ZenodoDeposit deposit) {
        PreviewFieldGroup group = this.buildGroup(GROUP_ADDITIONAL_DESCRIPTIONS, "Additional descriptions");
        group.setRepeatable(true);
        List<PreviewField> fields = group.getFields();

        fields.add(this.buildTemplateField(ITEM_ADDITIONAL_DESCRIPTION_TEXT, "Description", PreviewFieldType.TextArea));
        PreviewField descriptionType = this.buildTemplateField(ITEM_ADDITIONAL_DESCRIPTION_TYPE, "Type", PreviewFieldType.Select);
        descriptionType.setOptions(DESCRIPTION_TYPE_OPTIONS);
        fields.add(descriptionType);

        List<PreviewGroupItemModel> items = new ArrayList<>();
        List<ZenodoAdditionalDescription> additionalDescriptions = deposit.getMetadata() != null ? deposit.getMetadata().getAdditionalDescriptions() : null;
        if (additionalDescriptions != null) {
            for (ZenodoAdditionalDescription additionalDescription : additionalDescriptions) {
                if (additionalDescription == null) continue;
                List<PreviewFieldValue> values = new ArrayList<>();
                values.add(this.textValue(ITEM_ADDITIONAL_DESCRIPTION_TEXT, additionalDescription.getDescription()));
                values.add(this.textValue(ITEM_ADDITIONAL_DESCRIPTION_TYPE, additionalDescription.getType() != null ? additionalDescription.getType().getId() : null));
                PreviewGroupItemModel item = new PreviewGroupItemModel();
                item.setValues(values);
                items.add(item);
            }
        }
        group.setItems(items);
        group.setBaseItems(items);
        return group;
    }

    private PreviewFieldGroup buildFilesGroup(List<String> uploadFileNames) {
        PreviewFieldGroup group = this.buildGroup(GROUP_FILES_CODE, "Files");
        group.getFields().add(this.buildField(FIELD_FILES, "Files to be uploaded", PreviewFieldType.ReadOnly, PreviewFieldSource.System, false,
                () -> this.textListValue(FIELD_FILES, uploadFileNames)));
        return group;
    }

    private PreviewFieldGroup buildDomainSpecificGroup(ZenodoDeposit deposit) {
        PreviewFieldGroup group = this.buildGroup(GROUP_DOMAIN_SPECIFIC_CODE, "Domain specific fields");
        List<PreviewField> fields = group.getFields();
        for (PreviewExtraFieldsProperties.ExtraField extraField : this.previewExtraFieldsProperties.getFields()) {
            if (extraField == null || extraField.getKey() == null || extraField.getKey().isBlank()) continue;
            String key = extraField.getKey();
            String label = extraField.getLabel() != null && !extraField.getLabel().isBlank() ? extraField.getLabel() : key;
            PreviewFieldType type = this.extraFieldType(extraField.getType());
            fields.add(this.buildField(key, label, type, PreviewFieldSource.Manual, false,
                    () -> this.extraFieldValue(key, type, deposit)));
        }
        return group;
    }

    private PreviewFieldType extraFieldType(String type) {
        if (type == null) return PreviewFieldType.Text;
        return switch (type.toLowerCase()) {
            case "textarea" -> PreviewFieldType.TextArea;
            case "tags" -> PreviewFieldType.Tags;
            case "boolean" -> PreviewFieldType.Boolean;
            case "number" -> PreviewFieldType.Number;
            case "date" -> PreviewFieldType.Date;
            default -> PreviewFieldType.Text;
        };
    }

    private PreviewFieldValue extraFieldValue(String key, PreviewFieldType type, ZenodoDeposit deposit) {
        PreviewFieldValue value = new PreviewFieldValue();
        value.setCode(key);
        Object raw = deposit.getCustomFields() != null ? deposit.getCustomFields().getAdditionalProperties().get(key) : null;
        if (raw == null) return value;
        switch (type) {
            case Tags -> {
                if (raw instanceof List<?> list) {
                    List<String> texts = new ArrayList<>();
                    for (Object entry : list) if (entry != null) texts.add(entry.toString());
                    value.setTextListValue(texts.isEmpty() ? null : texts);
                }
            }
            case Boolean -> { if (raw instanceof Boolean b) value.setBooleanValue(b); }
            case Number -> { if (raw instanceof Number n) value.setNumberValue(n.doubleValue()); }
            default -> value.setTextValue(raw.toString());
        }
        return value;
    }

    private PreviewFieldGroup buildAdditionalInformationGroup(ZenodoDeposit deposit, String community, List<String> organisationCommunities) {
        PreviewFieldGroup group = this.buildGroup("additional-information", "Additional information");
        List<PreviewField> fields = group.getFields();

        fields.add(this.buildField(FIELD_REFERENCES, "References", PreviewFieldType.Tags, PreviewFieldSource.Mapped, false,
                () -> this.textListValue(FIELD_REFERENCES, this.referenceLabels(deposit))));
        List<String> relatedWorks = this.relatedWorkLabels(deposit);
        if (relatedWorks != null && !relatedWorks.isEmpty()) {
            fields.add(this.buildField(FIELD_RELATED_WORKS, "Related works", PreviewFieldType.ReadOnly, PreviewFieldSource.System, false,
                    () -> this.textListValue(FIELD_RELATED_WORKS, relatedWorks)));
        }
        if (community != null && !community.isBlank()) {
            fields.add(this.buildField(FIELD_COMMUNITY, "Community", PreviewFieldType.ReadOnly, PreviewFieldSource.System, false,
                    () -> this.textValue(FIELD_COMMUNITY, community)));
        }
        PreviewField additionalCommunities = this.buildField(FIELD_ADDITIONAL_COMMUNITIES, "Additional communities", PreviewFieldType.Tags, PreviewFieldSource.Manual, false,
                () -> this.textListValue(FIELD_ADDITIONAL_COMMUNITIES, organisationCommunities.isEmpty() ? null : organisationCommunities));
        additionalCommunities.setVocabSearch(true);
        fields.add(additionalCommunities);

        return group;
    }

    public void applyOverrides(ZenodoDeposit deposit, List<PreviewFieldValue> editedFields, List<PreviewGroupValuesModel> editedGroups, DepositPluginSettings settings) {
        if (deposit == null) return;
        if (editedFields != null) {
            for (PreviewFieldValue value : editedFields) {
                if (value == null || value.getCode() == null || this.isLocked(value.getCode(), settings)) continue;
                this.applyFieldOverride(deposit, value);
            }
        }
        if (editedGroups != null) {
            for (PreviewGroupValuesModel group : editedGroups) {
                if (group == null || group.getGroupCode() == null) continue;
                switch (group.getGroupCode()) {
                    case GROUP_CREATORS -> {
                        List<ZenodoCreator> creators = this.buildCreatorsFromItems(group.getItems(), false);
                        if (creators != null && !creators.isEmpty()) deposit.getMetadata().setCreators(creators);
                    }
                    case GROUP_CONTRIBUTORS -> deposit.getMetadata().setContributors(this.buildCreatorsFromItems(group.getItems(), true));
                    case GROUP_FUNDING -> deposit.getMetadata().setFunding(this.buildFundingFromItems(group.getItems()));
                    case GROUP_ADDITIONAL_TITLES -> deposit.getMetadata().setAdditionalTitles(this.buildAdditionalTitlesFromItems(group.getItems()));
                    case GROUP_DATES -> deposit.getMetadata().setDates(this.buildDatesFromItems(group.getItems()));
                    case GROUP_ALTERNATE_IDENTIFIERS -> deposit.getMetadata().setAlternateIdentifiers(this.buildAlternateIdentifiersFromItems(group.getItems()));
                    case GROUP_ADDITIONAL_DESCRIPTIONS -> deposit.getMetadata().setAdditionalDescriptions(this.buildAdditionalDescriptionsFromItems(group.getItems()));
                    default -> {
                        // unknown group codes are ignored; they may come from a newer descriptor version
                    }
                }
            }
        }
    }

    private void applyFieldOverride(ZenodoDeposit deposit, PreviewFieldValue value) {
        switch (value.getCode()) {
            case FIELD_TITLE -> {
                if (this.hasText(value)) deposit.getMetadata().setTitle(value.getTextValue());
            }
            case FIELD_DESCRIPTION -> {
                if (this.hasText(value)) deposit.getMetadata().setDescription(value.getTextValue());
            }
            case FIELD_PUBLICATION_DATE -> {
                if (value.getDateValue() != null) deposit.getMetadata().setPublicationDate(DATE_FORMAT.format(value.getDateValue()));
            }
            case FIELD_VERSION -> {
                if (this.hasText(value)) deposit.getMetadata().setVersion(value.getTextValue());
            }
            case FIELD_PUBLISHER -> {
                if (this.hasText(value)) deposit.getMetadata().setPublisher(value.getTextValue());
            }
            case FIELD_LANGUAGE -> {
                List<String> ids = this.textListOrSingle(value);
                if (ids != null) {
                    List<ZenodoLanguage> languages = new ArrayList<>();
                    for (String id : ids) {
                        ZenodoLanguage language = new ZenodoLanguage();
                        language.setId(id);
                        languages.add(language);
                    }
                    deposit.getMetadata().setLanguages(languages.isEmpty() ? null : languages);
                }
            }
            case FIELD_KEYWORDS -> {
                if (value.getTextListValue() != null) {
                    List<ZenodoSubject> subjects = new ArrayList<>();
                    for (String keyword : value.getTextListValue()) {
                        if (keyword == null || keyword.isBlank()) continue;
                        ZenodoSubject subject = new ZenodoSubject();
                        subject.setSubject(keyword);
                        subjects.add(subject);
                    }
                    deposit.getMetadata().setSubjects(subjects.isEmpty() ? null : subjects);
                }
            }
            case FIELD_LICENSE -> {
                List<String> entries = this.textListOrSingle(value);
                if (entries != null) {
                    List<ZenodoLicense> licenses = new ArrayList<>();
                    for (String entry : entries) {
                        ZenodoLicense license = new ZenodoLicense();
                        if (LICENSE_ID_PATTERN.matcher(entry).matches()) {
                            license.setId(entry);
                        } else {
                            ZenodoLicense.Title title = new ZenodoLicense.Title();
                            title.setEn(entry);
                            license.setTitle(title);
                        }
                        licenses.add(license);
                    }
                    deposit.getMetadata().setLicense(licenses.isEmpty() ? null : licenses);
                }
            }
            case FIELD_ACCESS_FILES -> {
                if (this.hasText(value) && deposit.getAccess() != null) {
                    deposit.getAccess().setFiles(ZenodoAccessRight.Names.Public.equals(value.getTextValue()) ? ZenodoAccessRight.PUBLIC : ZenodoAccessRight.RESTRICTED);
                }
            }
            case FIELD_EMBARGO_UNTIL -> {
                if (value.getDateValue() != null && deposit.getAccess() != null) {
                    ZenodoDepositAccess.Embargo embargo = deposit.getAccess().getEmbargo();
                    if (embargo == null) {
                        embargo = new ZenodoDepositAccess.Embargo();
                        deposit.getAccess().setEmbargo(embargo);
                    }
                    embargo.setActive(true);
                    embargo.setUntil(DATE_FORMAT.format(value.getDateValue()));
                }
            }
            case FIELD_EMBARGO_REASON -> {
                if (this.hasText(value) && deposit.getAccess() != null && deposit.getAccess().getEmbargo() != null) {
                    deposit.getAccess().getEmbargo().setReason(value.getTextValue());
                }
            }
            case FIELD_ADDITIONAL_DESCRIPTION -> {
                if (this.hasText(value)) {
                    ZenodoAdditionalDescription additionalDescription = new ZenodoAdditionalDescription();
                    additionalDescription.setDescription(value.getTextValue());
                    ZenodoAdditionalDescription.Type type = new ZenodoAdditionalDescription.Type();
                    type.setId(ADDITIONAL_DESCRIPTION_TYPE_OTHER);
                    additionalDescription.setType(type);
                    deposit.getMetadata().setAdditionalDescriptions(List.of(additionalDescription));
                }
            }
            case FIELD_REFERENCES -> {
                if (value.getTextListValue() != null) {
                    List<ZenodoReference> references = new ArrayList<>();
                    for (String entry : value.getTextListValue()) {
                        if (entry == null || entry.isBlank()) continue;
                        ZenodoReference reference = new ZenodoReference();
                        reference.setReference(entry);
                        references.add(reference);
                    }
                    deposit.getMetadata().setReferences(references.isEmpty() ? null : references);
                }
            }
            case FIELD_COPYRIGHT -> {
                if (this.hasText(value)) deposit.getMetadata().setCopyright(value.getTextValue());
            }
            case FIELD_JOURNAL_TITLE -> { if (this.hasText(value)) this.journal(deposit).setTitle(value.getTextValue()); }
            case FIELD_JOURNAL_ISSN -> { if (this.hasText(value)) this.journal(deposit).setIssn(value.getTextValue()); }
            case FIELD_JOURNAL_VOLUME -> { if (this.hasText(value)) this.journal(deposit).setVolume(value.getTextValue()); }
            case FIELD_JOURNAL_ISSUE -> { if (this.hasText(value)) this.journal(deposit).setIssue(value.getTextValue()); }
            case FIELD_JOURNAL_PAGES -> { if (this.hasText(value)) this.journal(deposit).setPages(value.getTextValue()); }
            case FIELD_IMPRINT_TITLE -> { if (this.hasText(value)) this.imprint(deposit).setTitle(value.getTextValue()); }
            case FIELD_IMPRINT_ISBN -> { if (this.hasText(value)) this.imprint(deposit).setIsbn(value.getTextValue()); }
            case FIELD_IMPRINT_PLACE -> { if (this.hasText(value)) this.imprint(deposit).setPlace(value.getTextValue()); }
            case FIELD_IMPRINT_PAGES -> { if (this.hasText(value)) this.imprint(deposit).setPages(value.getTextValue()); }
            case FIELD_IMPRINT_EDITION -> { if (this.hasText(value)) this.imprint(deposit).setEdition(value.getTextValue()); }
            case FIELD_THESIS_UNIVERSITY -> { if (this.hasText(value)) this.thesis(deposit).setUniversity(value.getTextValue()); }
            case FIELD_THESIS_DEPARTMENT -> { if (this.hasText(value)) this.thesis(deposit).setDepartment(value.getTextValue()); }
            case FIELD_THESIS_TYPE -> { if (this.hasText(value)) this.thesis(deposit).setType(value.getTextValue()); }
            case FIELD_THESIS_DATE_SUBMITTED -> { if (this.hasText(value)) this.thesis(deposit).setDateSubmitted(value.getTextValue()); }
            case FIELD_THESIS_DATE_DEFENDED -> { if (this.hasText(value)) this.thesis(deposit).setDateDefended(value.getTextValue()); }
            case FIELD_CONFERENCE_TITLE -> { if (this.hasText(value)) this.conference(deposit).setTitle(value.getTextValue()); }
            case FIELD_CONFERENCE_ACRONYM -> { if (this.hasText(value)) this.conference(deposit).setAcronym(value.getTextValue()); }
            case FIELD_CONFERENCE_PLACE -> { if (this.hasText(value)) this.conference(deposit).setPlace(value.getTextValue()); }
            case FIELD_CONFERENCE_DATES -> { if (this.hasText(value)) this.conference(deposit).setDates(value.getTextValue()); }
            case FIELD_CONFERENCE_SESSION -> { if (this.hasText(value)) this.conference(deposit).setSession(value.getTextValue()); }
            case FIELD_CONFERENCE_PART -> { if (this.hasText(value)) this.conference(deposit).setSessionPart(value.getTextValue()); }
            case FIELD_CONFERENCE_WEBSITE -> { if (this.hasText(value)) this.conference(deposit).setUrl(value.getTextValue()); }
            case FIELD_SOFTWARE_REPOSITORY_URL -> { if (this.hasText(value)) this.customFields(deposit).setCodeRepository(value.getTextValue()); }
            case FIELD_SOFTWARE_PROGRAMMING_LANGUAGE -> {
                if (this.hasText(value)) {
                    ZenodoDepositCustomFields.Code code = new ZenodoDepositCustomFields.Code();
                    code.setId(value.getTextValue());
                    this.customFields(deposit).setCodeProgrammingLanguage(List.of(code));
                }
            }
            case FIELD_SOFTWARE_DEVELOPMENT_STATUS -> {
                if (this.hasText(value)) {
                    ZenodoDepositCustomFields.Code code = new ZenodoDepositCustomFields.Code();
                    code.setId(value.getTextValue());
                    this.customFields(deposit).setCodeDevelopmentStatus(code);
                }
            }
            case FIELD_ADDITIONAL_COMMUNITIES -> {
                // communities are not part of the deposit document; the service applies them post-publish
            }
            default -> this.applyExtraFieldOverride(deposit, value);
        }
    }

    private void applyExtraFieldOverride(ZenodoDeposit deposit, PreviewFieldValue value) {
        // read-only and codes from a newer descriptor version fall through here and are ignored,
        // unless they match a configured deployment-specific extra custom field
        PreviewExtraFieldsProperties.ExtraField extraField = this.previewExtraFieldsProperties.getFields() != null
                ? this.previewExtraFieldsProperties.getFields().stream().filter(x -> x != null && value.getCode().equals(x.getKey())).findFirst().orElse(null)
                : null;
        if (extraField == null) return;

        Object converted = switch (this.extraFieldType(extraField.getType())) {
            case Tags -> {
                List<String> texts = this.textListOrSingle(value);
                yield texts == null || texts.isEmpty() ? null : texts;
            }
            case Boolean -> value.getBooleanValue();
            case Number -> value.getNumberValue();
            case Date -> value.getDateValue() != null ? DATE_FORMAT.format(value.getDateValue()) : null;
            default -> this.hasText(value) ? value.getTextValue() : null;
        };
        if (converted != null) this.customFields(deposit).setAdditionalProperty(extraField.getKey(), converted);
    }

    private boolean isLocked(String fieldCode, DepositPluginSettings settings) {
        return LOCKABLE_FIELDS.containsKey(fieldCode) && settings.isLocked(LOCKABLE_FIELDS.get(fieldCode));
    }

    /** Pulls the additional-communities override out of the edited fields; used by the service post-publish. */
    public List<String> extractAdditionalCommunities(List<PreviewFieldValue> editedFields) {
        if (editedFields == null) return List.of();
        for (PreviewFieldValue value : editedFields) {
            if (value == null || !FIELD_ADDITIONAL_COMMUNITIES.equals(value.getCode())) continue;
            List<String> slugs = this.textListOrSingle(value);
            return slugs != null ? slugs : List.of();
        }
        return List.of();
    }

    private ZenodoDepositCustomFields customFields(ZenodoDeposit deposit) {
        if (deposit.getCustomFields() == null) deposit.setCustomFields(new ZenodoDepositCustomFields());
        return deposit.getCustomFields();
    }

    private ZenodoDepositCustomFields.Journal journal(ZenodoDeposit deposit) {
        ZenodoDepositCustomFields customFields = this.customFields(deposit);
        if (customFields.getJournal() == null) customFields.setJournal(new ZenodoDepositCustomFields.Journal());
        return customFields.getJournal();
    }

    private ZenodoDepositCustomFields.Imprint imprint(ZenodoDeposit deposit) {
        ZenodoDepositCustomFields customFields = this.customFields(deposit);
        if (customFields.getImprint() == null) customFields.setImprint(new ZenodoDepositCustomFields.Imprint());
        return customFields.getImprint();
    }

    private ZenodoDepositCustomFields.Thesis thesis(ZenodoDeposit deposit) {
        ZenodoDepositCustomFields customFields = this.customFields(deposit);
        if (customFields.getThesis() == null) customFields.setThesis(new ZenodoDepositCustomFields.Thesis());
        return customFields.getThesis();
    }

    private ZenodoDepositCustomFields.Conference conference(ZenodoDeposit deposit) {
        ZenodoDepositCustomFields customFields = this.customFields(deposit);
        if (customFields.getConference() == null) customFields.setConference(new ZenodoDepositCustomFields.Conference());
        return customFields.getConference();
    }

    private List<ZenodoCreator> buildCreatorsFromItems(List<PreviewGroupItemModel> items, boolean withRole) {
        if (items == null || items.isEmpty()) return null;
        List<ZenodoCreator> creators = new ArrayList<>();
        for (PreviewGroupItemModel item : items) {
            if (item == null || item.getValues() == null) continue;
            String type = this.itemText(item, ITEM_PERSON_TYPE);
            String givenName = this.itemText(item, ITEM_GIVEN_NAME);
            String familyName = this.itemText(item, ITEM_FAMILY_NAME);
            String name = this.itemText(item, ITEM_NAME);
            String orcid = this.itemText(item, ITEM_ORCID);
            String affiliation = this.itemText(item, ITEM_AFFILIATION);
            if (givenName == null && familyName == null && name == null) continue;

            ZenodoCreator creator = new ZenodoCreator();
            ZenodoCreator.PersonOrOrg person = new ZenodoCreator.PersonOrOrg();
            person.setType(type != null ? type : (givenName != null || familyName != null ? PERSON_TYPE_PERSONAL : PERSON_TYPE_ORGANIZATIONAL));
            person.setGivenName(givenName);
            person.setFamilyName(familyName);
            person.setName(name != null ? name : String.join(" ",
                    familyName != null ? familyName : "",
                    givenName != null ? givenName : "").trim());
            if (orcid != null) {
                ZenodoCreator.PersonOrOrg.Identifier identifier = new ZenodoCreator.PersonOrOrg.Identifier();
                identifier.setScheme(ORCID_SCHEME);
                identifier.setIdentifier(orcid);
                person.setIdentifiers(List.of(identifier));
            }
            creator.setPersonOrOrg(person);
            if (affiliation != null) creator.setAffiliations(List.of(new ZenodoCreator.Affiliation(affiliation)));
            if (withRole) {
                String role = this.itemText(item, ITEM_ROLE);
                creator.setRole(new ZenodoCreator.Role(role != null ? role : "other"));
            }
            creators.add(creator);
        }
        return creators.isEmpty() ? null : creators;
    }

    private List<ZenodoFunding> buildFundingFromItems(List<PreviewGroupItemModel> items) {
        if (items == null || items.isEmpty()) return null;
        List<ZenodoFunding> fundingList = new ArrayList<>();
        for (PreviewGroupItemModel item : items) {
            if (item == null || item.getValues() == null) continue;
            String funderName = this.itemText(item, ITEM_FUNDER_NAME);
            String funderId = this.itemText(item, ITEM_FUNDER_ID);
            String awardTitle = this.itemText(item, ITEM_AWARD_TITLE);
            String awardNumber = this.itemText(item, ITEM_AWARD_NUMBER);
            if (funderName == null && funderId == null) continue;

            ZenodoFunding funding = new ZenodoFunding();
            ZenodoFunding.Funder funder = new ZenodoFunding.Funder();
            funder.setName(funderName);
            funder.setId(funderId);
            funding.setFunder(funder);
            if (awardTitle != null || awardNumber != null) {
                ZenodoFunding.Grant grant = new ZenodoFunding.Grant();
                grant.setTitle(awardTitle);
                grant.setNumber(awardNumber);
                funding.setGrant(grant);
            }
            fundingList.add(funding);
        }
        return fundingList.isEmpty() ? null : fundingList;
    }

    private List<ZenodoAdditionalTitle> buildAdditionalTitlesFromItems(List<PreviewGroupItemModel> items) {
        if (items == null || items.isEmpty()) return null;
        List<ZenodoAdditionalTitle> additionalTitles = new ArrayList<>();
        for (PreviewGroupItemModel item : items) {
            if (item == null || item.getValues() == null) continue;
            String title = this.itemText(item, ITEM_ADDITIONAL_TITLE);
            if (title == null) continue;

            ZenodoAdditionalTitle additionalTitle = new ZenodoAdditionalTitle();
            additionalTitle.setTitle(title);
            ZenodoAdditionalTitle.Type type = new ZenodoAdditionalTitle.Type();
            String typeValue = this.itemText(item, ITEM_ADDITIONAL_TITLE_TYPE);
            type.setId(typeValue != null && Arrays.stream(ZenodoAdditionalTitleIdType.values()).map(ZenodoAdditionalTitleIdType::getValue).toList().contains(typeValue) ? ZenodoAdditionalTitleIdType.of(typeValue) : ZenodoAdditionalTitleIdType.OTHER);
            additionalTitle.setType(type);
            String lang = this.itemText(item, ITEM_ADDITIONAL_TITLE_LANG);
            if (lang != null) {
                ZenodoAdditionalTitle.Language language = new ZenodoAdditionalTitle.Language();
                language.setId(lang);
                additionalTitle.setLang(language);
            }
            additionalTitles.add(additionalTitle);
        }
        return additionalTitles.isEmpty() ? null : additionalTitles;
    }

    private List<ZenodoDates> buildDatesFromItems(List<PreviewGroupItemModel> items) {
        if (items == null || items.isEmpty()) return null;
        List<ZenodoDates> dates = new ArrayList<>();
        for (PreviewGroupItemModel item : items) {
            if (item == null || item.getValues() == null) continue;
            String dateValue = this.itemText(item, ITEM_DATE_VALUE);
            if (dateValue == null) continue;

            ZenodoDates date = new ZenodoDates();
            date.setDate(dateValue);
            ZenodoDates.Type type = new ZenodoDates.Type();
            String typeValue = this.itemText(item, ITEM_DATE_TYPE);
            type.setId(typeValue != null ? typeValue : "other");
            date.setType(type);
            date.setDescription(this.itemText(item, ITEM_DATE_DESCRIPTION));
            dates.add(date);
        }
        return dates.isEmpty() ? null : dates;
    }

    private List<ZenodoAlternateIdentifiers> buildAlternateIdentifiersFromItems(List<PreviewGroupItemModel> items) {
        if (items == null || items.isEmpty()) return null;
        List<ZenodoAlternateIdentifiers> alternateIdentifiers = new ArrayList<>();
        for (PreviewGroupItemModel item : items) {
            if (item == null || item.getValues() == null) continue;
            String identifier = this.itemText(item, ITEM_IDENTIFIER_VALUE);
            if (identifier == null) continue;

            ZenodoAlternateIdentifiers alternateIdentifier = new ZenodoAlternateIdentifiers();
            alternateIdentifier.setIdentifier(identifier);
            alternateIdentifier.setScheme(this.itemText(item, ITEM_IDENTIFIER_SCHEME));
            alternateIdentifiers.add(alternateIdentifier);
        }
        return alternateIdentifiers.isEmpty() ? null : alternateIdentifiers;
    }

    private List<ZenodoAdditionalDescription> buildAdditionalDescriptionsFromItems(List<PreviewGroupItemModel> items) {
        if (items == null || items.isEmpty()) return null;
        List<ZenodoAdditionalDescription> additionalDescriptions = new ArrayList<>();
        for (PreviewGroupItemModel item : items) {
            if (item == null || item.getValues() == null) continue;
            String text = this.itemText(item, ITEM_ADDITIONAL_DESCRIPTION_TEXT);
            if (text == null) continue;

            ZenodoAdditionalDescription additionalDescription = new ZenodoAdditionalDescription();
            additionalDescription.setDescription(text);
            ZenodoAdditionalDescription.Type type = new ZenodoAdditionalDescription.Type();
            String typeValue = this.itemText(item, ITEM_ADDITIONAL_DESCRIPTION_TYPE);
            type.setId(typeValue != null ? typeValue : ADDITIONAL_DESCRIPTION_TYPE_OTHER);
            additionalDescription.setType(type);
            additionalDescriptions.add(additionalDescription);
        }
        return additionalDescriptions.isEmpty() ? null : additionalDescriptions;
    }

    private List<String> textListOrSingle(PreviewFieldValue value) {
        if (value.getTextListValue() != null) {
            List<String> ids = new ArrayList<>();
            for (String entry : value.getTextListValue()) {
                if (entry != null && !entry.isBlank()) ids.add(entry);
            }
            return ids;
        }
        if (this.hasText(value)) return List.of(value.getTextValue());
        return null;
    }

    private List<String> relatedWorkLabels(ZenodoDeposit deposit) {
        if (deposit.getMetadata() == null || deposit.getMetadata().getRelatedIdentifiers() == null) return null;
        List<String> labels = new ArrayList<>();
        for (ZenodoRelator relator : deposit.getMetadata().getRelatedIdentifiers()) {
            if (relator == null || relator.getIdentifier() == null || relator.getIdentifier().isBlank()) continue;
            String relation = relator.getRelationType() != null && relator.getRelationType().getId() != null ? relator.getRelationType().getId() : "related";
            labels.add(relation + ": " + relator.getIdentifier());
        }
        return labels;
    }

    private String firstProgrammingLanguageId(ZenodoDepositCustomFields customFields) {
        if (customFields == null || customFields.getCodeProgrammingLanguage() == null || customFields.getCodeProgrammingLanguage().isEmpty()) return null;
        ZenodoDepositCustomFields.Code code = customFields.getCodeProgrammingLanguage().getFirst();
        return code != null ? code.getId() : null;
    }

    private String itemText(PreviewGroupItemModel item, String code) {
        for (PreviewFieldValue value : item.getValues()) {
            if (value != null && code.equals(value.getCode())) {
                return value.getTextValue() != null && !value.getTextValue().isBlank() ? value.getTextValue() : null;
            }
        }
        return null;
    }

    private PreviewFieldGroup buildGroup(String code, String label) {
        PreviewFieldGroup group = new PreviewFieldGroup();
        group.setCode(code);
        group.setLabel(label);
        group.setFields(new ArrayList<>());
        return group;
    }

    private PreviewField buildField(String code, String label, PreviewFieldType type, PreviewFieldSource source, boolean required, java.util.function.Supplier<PreviewFieldValue> valueSupplier) {
        PreviewField field = new PreviewField();
        field.setCode(code);
        field.setLabel(label);
        field.setType(type);
        field.setSource(source);
        field.setRequired(required);
        field.setValue(valueSupplier.get());
        field.setBaseValue(valueSupplier.get());
        return field;
    }

    private PreviewField buildTemplateField(String code, String label, PreviewFieldType type) {
        PreviewField field = new PreviewField();
        field.setCode(code);
        field.setLabel(label);
        field.setType(type);
        field.setSource(PreviewFieldSource.Mapped);
        return field;
    }

    private PreviewFieldValue textValue(String code, String text) {
        PreviewFieldValue value = new PreviewFieldValue();
        value.setCode(code);
        value.setTextValue(text);
        return value;
    }

    private PreviewFieldValue textListValue(String code, List<String> texts) {
        PreviewFieldValue value = new PreviewFieldValue();
        value.setCode(code);
        value.setTextListValue(texts != null && !texts.isEmpty() ? texts : null);
        return value;
    }

    private PreviewFieldValue dateValue(String code, String formattedDate) {
        PreviewFieldValue value = new PreviewFieldValue();
        value.setCode(code);
        if (formattedDate != null && !formattedDate.isBlank()) {
            try {
                value.setDateValue(LocalDate.parse(formattedDate).atStartOfDay(ZoneOffset.UTC).toInstant());
            } catch (Exception e) {
                // leave the value empty if the mapped date is not in yyyy-MM-dd form
            }
        }
        return value;
    }

    private boolean hasText(PreviewFieldValue value) {
        return value.getTextValue() != null && !value.getTextValue().isBlank();
    }

    private String findIdentifier(ZenodoCreator.PersonOrOrg person, String scheme) {
        if (person.getIdentifiers() == null) return null;
        for (ZenodoCreator.PersonOrOrg.Identifier identifier : person.getIdentifiers()) {
            if (identifier != null && scheme.equalsIgnoreCase(identifier.getScheme())) return identifier.getIdentifier();
        }
        return null;
    }

    private List<String> languageIds(ZenodoDeposit deposit) {
        if (deposit.getMetadata() == null || deposit.getMetadata().getLanguages() == null) return null;
        List<String> ids = new ArrayList<>();
        for (ZenodoLanguage language : deposit.getMetadata().getLanguages()) {
            if (language != null && language.getId() != null && !language.getId().isBlank()) ids.add(language.getId());
        }
        return ids;
    }

    private List<String> licenseIds(ZenodoDeposit deposit) {
        if (deposit.getMetadata() == null || deposit.getMetadata().getLicense() == null) return null;
        List<String> ids = new ArrayList<>();
        for (ZenodoLicense license : deposit.getMetadata().getLicense()) {
            if (license == null) continue;
            if (license.getId() != null && !license.getId().isBlank()) ids.add(license.getId());
            else if (license.getTitle() != null && license.getTitle().getEn() != null && !license.getTitle().getEn().isBlank()) ids.add(license.getTitle().getEn());
        }
        return ids;
    }

    private List<String> subjectLabels(ZenodoDeposit deposit) {
        if (deposit.getMetadata() == null || deposit.getMetadata().getSubjects() == null) return null;
        List<String> labels = new ArrayList<>();
        for (ZenodoSubject subject : deposit.getMetadata().getSubjects()) {
            if (subject == null) continue;
            if (subject.getSubject() != null && !subject.getSubject().isBlank()) labels.add(subject.getSubject());
            else if (subject.getId() != null && !subject.getId().isBlank()) labels.add(subject.getId());
        }
        return labels;
    }

    private List<String> referenceLabels(ZenodoDeposit deposit) {
        if (deposit.getMetadata() == null || deposit.getMetadata().getReferences() == null) return null;
        List<String> labels = new ArrayList<>();
        for (ZenodoReference reference : deposit.getMetadata().getReferences()) {
            if (reference != null && reference.getReference() != null && !reference.getReference().isBlank()) labels.add(reference.getReference());
        }
        return labels;
    }
}

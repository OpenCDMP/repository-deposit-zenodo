package org.opencdmp.deposit.zenodorepository.configuration.semantics;

import org.opencdmp.commonmodels.enums.PluginEntityType;
import org.opencdmp.commonmodels.enums.SemanticKeyLevel;
import org.opencdmp.commonmodels.enums.SemanticValueFormat;
import org.opencdmp.commonmodels.models.semantics.SemanticKeyUsage;
import org.opencdmp.commonmodels.models.semantics.SemanticObject;
import org.opencdmp.commonmodels.models.semantics.SemanticVocabulary;
import org.opencdmp.commonmodels.models.semantics.SemanticsManifest;
import org.opencdmp.deposit.zenodorepository.configuration.programminglanguages.ProgrammingLanguagesProperties;
import org.opencdmp.deposit.zenodorepository.enums.ZenodoAdditionalTitleIdType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The semantics manifest the Zenodo deposit publishes with its configuration: one usage per value the deposit reads
 * from blueprint and template inputs, with the objects (additional title, journal, imprint, conference) they make up.
 * <p>
 * Every value is "can use": the deposit always writes the title, resource type, publisher, publication date,
 * description and creators from the plan and its configuration, so nothing a template or blueprint leaves out breaks
 * it. Keys are read from blueprint and template inputs alike, except the contributor names and the licence link, which
 * are read from the fields of the plan's researcher and licence references. A select or radio input gives the value of
 * the chosen option (its label only when the option has no value), so choice keys match on option values. The journal,
 * imprint and conference objects are written only when at least one of their values is answered.
 */
public final class ZenodoSemanticsManifest {

    public static final String PREFIX = "zenodo";

    private static final List<PluginEntityType> PLAN_AND_DESCRIPTION = List.of(PluginEntityType.Plan, PluginEntityType.Description);
    private static final List<PluginEntityType> PLAN = List.of(PluginEntityType.Plan);

    private static final String OBJECT_ADDITIONAL_TITLE = "zenodo.additional_title";
    private static final String OBJECT_JOURNAL = "zenodo.publishing_information.journal";
    private static final String OBJECT_IMPRINT = "zenodo.publishing_information.imprint";
    private static final String OBJECT_CONFERENCE = "zenodo.conference";

    private ZenodoSemanticsManifest() {
    }

    public static SemanticsManifest build(SemanticsProperties semantics, ProgrammingLanguagesProperties languages) {
        List<SemanticKeyUsage> usages = new ArrayList<>();

        SemanticKeyUsage community = usage("zenodo.community", "Community", "The community a new deposit is submitted to after publishing; not sent again for a new version.", "community", SemanticValueFormat.Text, false);
        community.setFallback("The configured community");
        usages.add(community);

        SemanticKeyUsage publicationDate = usage("zenodo.publication_date", "Publication date", "Written as the publication date; a date (yyyy-MM-dd or yyyy/MM/dd) or an ISO date-time, e.g. 2024-01-01T00:00:00Z. The first valid answer is used, anything else is dropped.", "metadata.publication_date", SemanticValueFormat.Date, false);
        publicationDate.setFallback("Today");
        usages.add(publicationDate);

        usages.add(usage("zenodo.subject", "Subject", "Added to the subjects (keywords) of the deposit.", "metadata.subjects[].subject", SemanticValueFormat.Text, true));
        usages.add(usage("zenodo.language", "Language", "Added to the languages of the deposit; a value that is not a three-letter ISO 639 code is dropped.", "metadata.languages[].id", SemanticValueFormat.LanguageThreeLetter, true));
        usages.add(usage("zenodo.reference", "Reference", "Added to the references of the deposit.", "metadata.references[].reference", SemanticValueFormat.Text, true));

        // additional title: the deposit finds the field sets carrying these keys and writes one title per answer
        SemanticKeyUsage additionalTitle = usage("zenodo.additional_title.title", "Additional title › Title", "Written as the text of an additional title; put the keys of one title on the inputs of the same field set. An answer without a title is skipped, whatever its type or language.", "metadata.additional_titles[].title", SemanticValueFormat.Text, true);
        additionalTitle.setObject(OBJECT_ADDITIONAL_TITLE);
        usages.add(additionalTitle);
        SemanticKeyUsage additionalTitleType = usage("zenodo.additional_title.type", "Additional title › Type", "Written as the type of an additional title; an unknown value is written as other.", "metadata.additional_titles[].type.id", SemanticValueFormat.Choice, true);
        additionalTitleType.setValues(Arrays.stream(ZenodoAdditionalTitleIdType.values()).map(ZenodoAdditionalTitleIdType::getValue).toList());
        additionalTitleType.setFallback("other");
        additionalTitleType.setObject(OBJECT_ADDITIONAL_TITLE);
        usages.add(additionalTitleType);
        SemanticKeyUsage additionalTitleLanguage = usage("zenodo.additional_title.language", "Additional title › Language", "Written as the language of an additional title; a value that is not a three-letter ISO 639 code is dropped.", "metadata.additional_titles[].lang.id", SemanticValueFormat.LanguageThreeLetter, true);
        additionalTitleLanguage.setObject(OBJECT_ADDITIONAL_TITLE);
        usages.add(additionalTitleLanguage);

        usages.add(objectValue(OBJECT_JOURNAL, "title", "Journal › Title", "custom_fields.journal:journal.title", SemanticValueFormat.Text));
        usages.add(objectValue(OBJECT_JOURNAL, "issn", "Journal › ISSN", "custom_fields.journal:journal.issn", SemanticValueFormat.Text));
        usages.add(objectValue(OBJECT_JOURNAL, "volume", "Journal › Volume", "custom_fields.journal:journal.volume", SemanticValueFormat.Text));
        usages.add(objectValue(OBJECT_JOURNAL, "issue", "Journal › Issue", "custom_fields.journal:journal.issue", SemanticValueFormat.Text));
        usages.add(objectValue(OBJECT_JOURNAL, "page_range_or_article_number", "Journal › Page range or article number", "custom_fields.journal:journal.pages", SemanticValueFormat.Text));

        usages.add(objectValue(OBJECT_IMPRINT, "title", "Imprint › Title", "custom_fields.imprint:imprint.title", SemanticValueFormat.Text));
        usages.add(objectValue(OBJECT_IMPRINT, "isbn", "Imprint › ISBN", "custom_fields.imprint:imprint.isbn", SemanticValueFormat.Text));
        usages.add(objectValue(OBJECT_IMPRINT, "place", "Imprint › Place", "custom_fields.imprint:imprint.place", SemanticValueFormat.Text));
        usages.add(objectValue(OBJECT_IMPRINT, "pagination", "Imprint › Pagination", "custom_fields.imprint:imprint.pages", SemanticValueFormat.Text));

        usages.add(usage("zenodo.publishing_information.thesis.university", "Thesis › University", "Written as the awarding university of the thesis.", "custom_fields.thesis:thesis.university", SemanticValueFormat.Text, false));

        usages.add(objectValue(OBJECT_CONFERENCE, "title", "Conference › Title", "custom_fields.meeting:meeting.title", SemanticValueFormat.Text));
        usages.add(objectValue(OBJECT_CONFERENCE, "acronym", "Conference › Acronym", "custom_fields.meeting:meeting.acronym", SemanticValueFormat.Text));
        usages.add(objectValue(OBJECT_CONFERENCE, "place", "Conference › Place", "custom_fields.meeting:meeting.place", SemanticValueFormat.Text));
        usages.add(objectValue(OBJECT_CONFERENCE, "dates", "Conference › Dates", "custom_fields.meeting:meeting.dates", SemanticValueFormat.Text));
        SemanticKeyUsage conferenceWebsite = objectValue(OBJECT_CONFERENCE, "website", "Conference › Website", "custom_fields.meeting:meeting.url", SemanticValueFormat.Uri);
        conferenceWebsite.setPurpose("Written as the website of the conference; a value that is not a valid URL is dropped.");
        usages.add(conferenceWebsite);
        usages.add(objectValue(OBJECT_CONFERENCE, "session", "Conference › Session", "custom_fields.meeting:meeting.session", SemanticValueFormat.Text));
        usages.add(objectValue(OBJECT_CONFERENCE, "part", "Conference › Session part", "custom_fields.meeting:meeting.session_part", SemanticValueFormat.Text));

        usages.add(usage("zenodo.software.repository-url", "Software › Repository URL", "Written as the code repository of the deposit; a value that is not a valid URL is dropped.", "custom_fields.code:codeRepository", SemanticValueFormat.Uri, false));
        SemanticKeyUsage programmingLanguage = usage("zenodo.software.programming-language", "Software › Programming language", "Added to the programming languages of the deposit; a value outside the accepted codes is dropped.", "custom_fields.code:programmingLanguage[].id", SemanticValueFormat.Choice, true);
        programmingLanguage.setValues(values(languages == null ? null : languages.getProgrammingCodes()));
        usages.add(programmingLanguage);
        SemanticKeyUsage developmentStatus = usage("zenodo.software.development-status", "Software › Development status", "Written as the development status of the software; a value outside the accepted ones is dropped.", "custom_fields.code:developmentStatus.id", SemanticValueFormat.Choice, false);
        developmentStatus.setValues(values(languages == null ? null : languages.getDevelopmentStatus()));
        usages.add(developmentStatus);

        List<String> relatedIdentifiers = values(semantics == null ? null : semantics.getRelatedIdentifiers());
        if (relatedIdentifiers != null) {
            SemanticKeyUsage usage = new SemanticKeyUsage(relatedIdentifiers, false, SemanticKeyLevel.CanUse, PLAN_AND_DESCRIPTION, "Related identifier",
                    "Added to the related identifiers, one per answer, with the key's last part as the relation; put a scheme key on the same input. From a reference input, an organisation or researcher gives its reference id, and any other reference gives its PID when the PID type is an accepted one and its reference id otherwise; an answer with several references still gives one identifier. An empty answer is skipped.", "metadata.related_identifiers[]");
            usage.setMultiple(true);
            usages.add(usage);
        }

        List<String> relatedIdentifierSchemes = values(semantics == null ? null : semantics.getRelatedIdentifiersScheme());
        if (relatedIdentifierSchemes != null) {
            SemanticKeyUsage usage = new SemanticKeyUsage(relatedIdentifierSchemes, false, SemanticKeyLevel.CanUse, PLAN_AND_DESCRIPTION, "Related identifier › Scheme",
                    "Carried by a related identifier input: the scheme is the last part of the scheme key; without it the scheme is other.", "metadata.related_identifiers[].scheme");
            usage.setMultiple(true);
            usage.setFallback("other");
            usages.add(usage);
        }

        List<String> dateTypes = values(semantics == null ? null : semantics.getDateTypes());
        if (dateTypes != null) {
            SemanticKeyUsage usage = new SemanticKeyUsage(dateTypes, false, SemanticKeyLevel.CanUse, PLAN_AND_DESCRIPTION, "Date",
                    "Added to the dates of the deposit, with the key's last part as the type; a value that is not a yyyy-MM-dd or yyyy/MM/dd date is dropped.", "metadata.dates[]");
            usage.setFormat(SemanticValueFormat.Date);
            usage.setMultiple(true);
            usages.add(usage);
        }

        List<String> alternateIdentifierSchemes = values(semantics == null ? null : semantics.getAlternateIdentifiersScheme());
        if (alternateIdentifierSchemes != null) {
            SemanticKeyUsage usage = new SemanticKeyUsage(alternateIdentifierSchemes, false, SemanticKeyLevel.CanUse, PLAN_AND_DESCRIPTION, "Alternate identifier",
                    "Added to the alternate identifiers of the deposit, with the key's last part as the scheme.", "metadata.identifiers[]");
            usage.setFormat(SemanticValueFormat.Text);
            usage.setMultiple(true);
            usages.add(usage);
        }

        SemanticKeyUsage familyName = referenceField("zenodo.metadata.contributors.person_or_org.family_name", "Contributor › Family name", "Read from a field of a researcher reference and written as the family name of the researcher contributor.", "metadata.contributors[].person_or_org.family_name");
        familyName.setFallback("The researcher's label");
        usages.add(familyName);
        SemanticKeyUsage givenName = referenceField("zenodo.metadata.contributors.person_or_org.given_name", "Contributor › Given name", "Read from a field of a researcher reference and written as the given name of the researcher contributor.", "metadata.contributors[].person_or_org.given_name");
        givenName.setFallback("Empty");
        usages.add(givenName);
        SemanticKeyUsage licenseLink = referenceField("zenodo.metadata.rights.link", "Rights › Link", "Read from a field of a licence reference; written as the licence link only when the licence is not found in Zenodo's vocabulary.", "metadata.rights[].link");
        licenseLink.setFormat(SemanticValueFormat.Uri);
        usages.add(licenseLink);

        SemanticsManifest manifest = new SemanticsManifest(List.of(new SemanticVocabulary(PREFIX, "Zenodo", false)), usages);
        manifest.setObjects(List.of(
                object(OBJECT_ADDITIONAL_TITLE, "Additional title", true, "metadata.additional_titles[]"),
                object(OBJECT_JOURNAL, "Journal", false, "custom_fields.journal:journal"),
                object(OBJECT_IMPRINT, "Imprint", false, "custom_fields.imprint:imprint"),
                object(OBJECT_CONFERENCE, "Conference", false, "custom_fields.meeting:meeting")
        ));
        return manifest;
    }

    private static SemanticKeyUsage usage(String key, String label, String purpose, String target, SemanticValueFormat format, boolean multiple) {
        SemanticKeyUsage usage = new SemanticKeyUsage(List.of(key), false, SemanticKeyLevel.CanUse, PLAN_AND_DESCRIPTION, label, purpose, target);
        usage.setFormat(format);
        usage.setMultiple(multiple);
        return usage;
    }

    /** A single value of a single custom-field object; the first answer found is written. */
    private static SemanticKeyUsage objectValue(String object, String name, String label, String target, SemanticValueFormat format) {
        String value = label.substring(label.lastIndexOf(" › ") + 3);
        String purpose = "Written as the " + (value.equals(value.toUpperCase()) ? value : value.toLowerCase()) + " of the " + label.substring(0, label.indexOf(" › ")).toLowerCase() + ".";
        SemanticKeyUsage usage = usage(object + "." + name, label, purpose, target, format, false);
        usage.setObject(object);
        return usage;
    }

    /** A key matched on a reference type's own field definitions, read from the plan's references only. */
    private static SemanticKeyUsage referenceField(String key, String label, String purpose, String target) {
        SemanticKeyUsage usage = new SemanticKeyUsage(List.of(key), false, SemanticKeyLevel.CanUse, PLAN, label, purpose, target);
        usage.setFormat(SemanticValueFormat.Text);
        usage.setFromReferenceFields(true);
        return usage;
    }

    private static SemanticObject object(String key, String label, boolean multiple, String target) {
        SemanticObject object = new SemanticObject();
        object.setKey(key);
        object.setLabel(label);
        object.setMultiple(multiple);
        object.setEntityTypes(PLAN_AND_DESCRIPTION);
        object.setTarget(target);
        return object;
    }

    private static List<String> values(List<String> values) {
        if (values == null) return null;
        List<String> distinct = values.stream().filter(Objects::nonNull).distinct().toList();
        return distinct.isEmpty() ? null : distinct;
    }
}

package org.opencdmp.deposit.zenodorepository.validation;

import org.opencdmp.deposit.zenodorepository.configuration.programminglanguages.ProgrammingLanguagesProperties;
import org.opencdmp.deposit.zenodorepository.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks a deposit the way Zenodo will, before anything is sent: identifiers by their scheme, dates, languages, URLs
 * and the fields Zenodo requires. Each problem is named by the review's section and row, so the user can find it.
 * Values from Zenodo's vocabularies (licences, roles, relation types...) are left to Zenodo, which answers for them.
 */
@Component
public class ZenodoDepositValidator {

    private static final Pattern EDTF_DATE = Pattern.compile("^(\\d{4})(?:-(\\d{2})(?:-(\\d{2}))?)?$");
    private static final Pattern LANGUAGE_ID = Pattern.compile("^[a-z]{3}$");
    private static final Pattern ROR_ID = Pattern.compile("^0[a-z0-9]{6}\\d{2}$");
    private static final Set<String> DEVELOPMENT_STATUSES = Set.of("concept", "wip", "active", "inactive", "unsupported", "moved", "suspended", "abandoned");
    private static final int TITLE_MIN_LENGTH = 3;
    private static final int VERSION_MAX_LENGTH = 191;

    private final ProgrammingLanguagesProperties programmingLanguagesProperties;

    public ZenodoDepositValidator(ProgrammingLanguagesProperties programmingLanguagesProperties) {
        this.programmingLanguagesProperties = programmingLanguagesProperties;
    }

    /** The problems by review label (e.g. "Creators, row 2"), in review order; empty when Zenodo should accept it. */
    public Map<String, List<String>> validate(ZenodoDeposit deposit) {
        Map<String, List<String>> problems = new LinkedHashMap<>();
        ZenodoDepositMetadata metadata = deposit.getMetadata();
        if (metadata == null) {
            add(problems, "Basic information", "The record has no metadata.");
            return problems;
        }

        String title = metadata.getTitle();
        if (title == null || title.trim().length() < TITLE_MIN_LENGTH) add(problems, "Title", "A title of at least " + TITLE_MIN_LENGTH + " characters is needed.");
        if (metadata.getPublicationDate() == null || metadata.getPublicationDate().isBlank()) add(problems, "Publication date", "A publication date is needed.");
        else if (!isEdtf(metadata.getPublicationDate())) add(problems, "Publication date", quote(metadata.getPublicationDate()) + " is not a date (YYYY-MM-DD, YYYY-MM or YYYY).");
        if (metadata.getVersion() != null && metadata.getVersion().length() > VERSION_MAX_LENGTH) add(problems, "Version", "At most " + VERSION_MAX_LENGTH + " characters.");
        if (metadata.getLanguages() != null) {
            for (ZenodoLanguage language : metadata.getLanguages()) {
                if (language != null && language.getId() != null && !LANGUAGE_ID.matcher(language.getId()).matches()) add(problems, "Languages", quote(language.getId()) + " is not a three-letter ISO 639-3 code (e.g. eng).");
            }
        }

        this.validateAccess(deposit.getAccess(), problems);

        if (metadata.getCreators() == null || metadata.getCreators().isEmpty()) add(problems, "Creators", "At least one creator is needed.");
        else this.validatePeople("Creators", metadata.getCreators(), false, problems);
        if (metadata.getContributors() != null) this.validatePeople("Contributors", metadata.getContributors(), true, problems);

        this.validateFunding(metadata.getFunding(), problems);
        this.validateAdditionalTitles(metadata.getAdditionalTitles(), problems);
        this.validateDates(metadata.getDates(), problems);
        this.validateAlternateIdentifiers(metadata.getAlternateIdentifiers(), problems);
        this.validateRelatedWorks(metadata.getRelatedIdentifiers(), problems);
        this.validateCustomFields(deposit.getCustomFields(), problems);
        return problems;
    }

    private void validateAccess(ZenodoDepositAccess access, Map<String, List<String>> problems) {
        if (access == null || access.getFiles() == null) {
            add(problems, "File access", "File access is needed.");
            return;
        }
        ZenodoDepositAccess.Embargo embargo = access.getEmbargo();
        if (embargo != null && embargo.isActive()) {
            LocalDate until = parseDate(embargo.getUntil());
            if (until == null) add(problems, "Embargo until", "An embargo end date (YYYY-MM-DD) is needed.");
            else if (!until.isAfter(LocalDate.now(ZoneOffset.UTC))) add(problems, "Embargo until", "The embargo must end in the future.");
        }
    }

    private void validatePeople(String section, List<ZenodoCreator> people, boolean roleRequired, Map<String, List<String>> problems) {
        for (int i = 0; i < people.size(); i++) {
            ZenodoCreator person = people.get(i);
            if (person == null) continue;
            String row = section + ", row " + (i + 1);
            ZenodoCreator.PersonOrOrg personOrOrg = person.getPersonOrOrg();
            if (personOrOrg == null) {
                add(problems, row, "A name is needed.");
                continue;
            }
            if ("organizational".equals(personOrOrg.getType())) {
                if (isBlank(personOrOrg.getName())) add(problems, row, "An organisation needs a name.");
            } else if (isBlank(personOrOrg.getFamilyName())) {
                add(problems, row, "A person needs a family name.");
            }
            if (personOrOrg.getIdentifiers() != null) {
                Set<String> schemes = new HashSet<>();
                for (ZenodoCreator.PersonOrOrg.Identifier identifier : personOrOrg.getIdentifiers()) {
                    if (identifier == null || isBlank(identifier.getIdentifier())) continue;
                    String scheme = identifier.getScheme() != null ? identifier.getScheme().toLowerCase(Locale.ROOT) : null;
                    if (!schemes.add(String.valueOf(scheme))) add(problems, row, "Only one " + label(scheme) + " per person.");
                    checkIdentifier(row, scheme, identifier.getIdentifier(), ZenodoIdentifiers.PERSON_OR_ORG_SCHEMES, problems);
                }
            }
            if (roleRequired && (person.getRole() == null || isBlank(person.getRole().getId()))) add(problems, row, "A role is needed.");
        }
    }

    private void validateFunding(List<ZenodoFunding> fundingList, Map<String, List<String>> problems) {
        if (fundingList == null) return;
        for (int i = 0; i < fundingList.size(); i++) {
            ZenodoFunding funding = fundingList.get(i);
            if (funding == null) continue;
            String row = "Funding, row " + (i + 1);
            ZenodoFunding.Funder funder = funding.getFunder();
            if (funder == null || (isBlank(funder.getId()) && isBlank(funder.getName()))) add(problems, row, "A funder is needed.");
            else if (!isBlank(funder.getId()) && !ROR_ID.matcher(funder.getId()).matches()) add(problems, row, quote(funder.getId()) + " is not a funder identifier: use the funder's ROR id (e.g. 00k4n6c32).");
            ZenodoFunding.Grant grant = funding.getGrant();
            if (grant != null && isBlank(grant.getId()) && isBlank(grant.getNumber()) && isBlank(grant.getTitle())) add(problems, row, "An award needs a number or a title.");
        }
    }

    private void validateAdditionalTitles(List<ZenodoAdditionalTitle> titles, Map<String, List<String>> problems) {
        if (titles == null) return;
        for (int i = 0; i < titles.size(); i++) {
            ZenodoAdditionalTitle title = titles.get(i);
            if (title == null) continue;
            String row = "Additional titles, row " + (i + 1);
            if (title.getTitle() == null || title.getTitle().trim().length() < TITLE_MIN_LENGTH) add(problems, row, "A title of at least " + TITLE_MIN_LENGTH + " characters is needed.");
            if (title.getLang() != null && title.getLang().getId() != null && !LANGUAGE_ID.matcher(title.getLang().getId()).matches()) add(problems, row, quote(title.getLang().getId()) + " is not a three-letter ISO 639-3 language code (e.g. eng).");
        }
    }

    private void validateDates(List<ZenodoDates> dates, Map<String, List<String>> problems) {
        if (dates == null) return;
        for (int i = 0; i < dates.size(); i++) {
            ZenodoDates date = dates.get(i);
            if (date == null) continue;
            String row = "Dates, row " + (i + 1);
            if (isBlank(date.getDate())) add(problems, row, "A date is needed.");
            else if (!isEdtf(date.getDate())) add(problems, row, quote(date.getDate()) + " is not a date or date range (YYYY-MM-DD, YYYY-MM, YYYY, or two of them as start/end).");
            if (date.getType() == null || isBlank(date.getType().getId())) add(problems, row, "A date type is needed.");
        }
    }

    private void validateAlternateIdentifiers(List<ZenodoAlternateIdentifiers> identifiers, Map<String, List<String>> problems) {
        if (identifiers == null) return;
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < identifiers.size(); i++) {
            ZenodoAlternateIdentifiers identifier = identifiers.get(i);
            if (identifier == null || isBlank(identifier.getIdentifier())) continue;
            String row = "Alternate identifiers, row " + (i + 1);
            if (!seen.add(identifier.getIdentifier().trim())) add(problems, row, quote(identifier.getIdentifier()) + " is listed more than once.");
            checkIdentifier(row, identifier.getScheme(), identifier.getIdentifier(), ZenodoIdentifiers.IDENTIFIER_SCHEMES, problems);
        }
    }

    private void validateRelatedWorks(List<ZenodoRelator> relators, Map<String, List<String>> problems) {
        if (relators == null) return;
        for (ZenodoRelator relator : relators) {
            if (relator == null || isBlank(relator.getIdentifier())) continue;
            checkIdentifier("Related works", relator.getScheme(), relator.getIdentifier(), ZenodoIdentifiers.IDENTIFIER_SCHEMES, problems);
            if (relator.getRelationType() == null || isBlank(relator.getRelationType().getId())) add(problems, "Related works", quote(relator.getIdentifier()) + " needs a relation type.");
        }
    }

    private void validateCustomFields(ZenodoDepositCustomFields customFields, Map<String, List<String>> problems) {
        if (customFields == null) return;
        if (customFields.getJournal() != null && !isBlank(customFields.getJournal().getIssn()) && !ZenodoIdentifiers.isIssn(customFields.getJournal().getIssn())) {
            add(problems, "Journal ISSN", quote(customFields.getJournal().getIssn()) + " is not a valid ISSN.");
        }
        if (customFields.getImprint() != null && !isBlank(customFields.getImprint().getIsbn()) && !ZenodoIdentifiers.isIsbn(customFields.getImprint().getIsbn())) {
            add(problems, "ISBN", quote(customFields.getImprint().getIsbn()) + " is not a valid ISBN.");
        }
        if (customFields.getConference() != null && !isBlank(customFields.getConference().getUrl()) && !ZenodoIdentifiers.isUrl(customFields.getConference().getUrl())) {
            add(problems, "Conference website", quote(customFields.getConference().getUrl()) + " is not a URL (e.g. https://example.org).");
        }
        if (!isBlank(customFields.getCodeRepository())) {
            if (!ZenodoIdentifiers.isUrl(customFields.getCodeRepository())) add(problems, "Repository URL", quote(customFields.getCodeRepository()) + " is not a URL (e.g. https://github.com/org/repo).");
            else if (customFields.getCodeRepository().contains("<") || customFields.getCodeRepository().contains(">")) add(problems, "Repository URL", "The URL cannot contain < or >.");
        }
        List<String> languages = this.programmingLanguagesProperties.getProgrammingCodes();
        if (customFields.getCodeProgrammingLanguage() != null && languages != null && !languages.isEmpty()) {
            for (ZenodoDepositCustomFields.Code code : customFields.getCodeProgrammingLanguage()) {
                if (code != null && !isBlank(code.getId()) && !languages.contains(code.getId())) add(problems, "Programming language", quote(code.getId()) + " is not a programming language Zenodo knows.");
            }
        }
        ZenodoDepositCustomFields.Code status = customFields.getCodeDevelopmentStatus();
        if (status != null && !isBlank(status.getId()) && !DEVELOPMENT_STATUSES.contains(status.getId())) add(problems, "Development status", quote(status.getId()) + " is not a development status Zenodo knows.");
    }

    private static void checkIdentifier(String label, String scheme, String value, Map<String, Predicate<String>> schemes, Map<String, List<String>> problems) {
        String key = scheme != null ? scheme.toLowerCase(Locale.ROOT) : null;
        if (isBlank(key)) {
            add(problems, label, quote(value) + " needs a scheme.");
            return;
        }
        Predicate<String> check = schemes.get(key);
        if (check == null) add(problems, label, quote(key) + " is not an identifier scheme Zenodo accepts.");
        else if (!check.test(value.trim())) add(problems, label, quote(value) + " is not a valid " + label(key) + ".");
    }

    /** An EDTF level 0 date (YYYY, YYYY-MM, YYYY-MM-DD) or interval of two of them. */
    static boolean isEdtf(String value) {
        String[] parts = value.trim().split("/", -1);
        if (parts.length > 2) return false;
        for (String part : parts) if (!isEdtfDate(part)) return false;
        return true;
    }

    private static boolean isEdtfDate(String value) {
        Matcher matcher = EDTF_DATE.matcher(value);
        if (!matcher.matches()) return false;
        try {
            int month = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 1;
            int day = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 1;
            LocalDate.of(Integer.parseInt(matcher.group(1)), month, day);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static LocalDate parseDate(String value) {
        try {
            return value != null ? LocalDate.parse(value.trim()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String label(String scheme) {
        if (scheme == null) return "identifier";
        return switch (scheme) {
            case "orcid" -> "ORCID";
            case "arxiv" -> "arXiv identifier";
            case "ads" -> "ADS bibcode";
            case "handle" -> "Handle";
            case "url" -> "URL";
            case "wikidata" -> "Wikidata id";
            case "gnd" -> "GND identifier";
            case "ror" -> "ROR id";
            default -> scheme.toUpperCase(Locale.ROOT) + " identifier";
        };
    }

    private static String quote(String value) {
        return "'" + value + "'";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void add(Map<String, List<String>> problems, String label, String message) {
        problems.computeIfAbsent(label, k -> new ArrayList<>()).add(message);
    }
}

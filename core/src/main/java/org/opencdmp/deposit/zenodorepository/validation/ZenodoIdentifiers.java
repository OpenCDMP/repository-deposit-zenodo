package org.opencdmp.deposit.zenodorepository.validation;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * The identifier checks Zenodo applies, ported from the idutils library InvenioRDM validates identifiers with
 * (RDM_RECORDS_PERSONORG_SCHEMES and RDM_RECORDS_IDENTIFIERS_SCHEMES). The regular expressions are idutils' own and,
 * like its {@code re.match}, match from the start of the value.
 */
public final class ZenodoIdentifiers {

    private static final Pattern DOI = Pattern.compile("(doi:\\s*|(?:https?://)?(?:dx\\.)?doi\\.org/)?(10\\.\\d+(\\.\\d+)*/.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern HANDLE = Pattern.compile("(hdl:\\s*|(?:https?://)?hdl\\.handle\\.net/)?([^/.]+(\\.[^/.]+)*/.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARK_SUFFIX = Pattern.compile("ark:/?[0-9bcdfghjkmnpqrstvwxz]+/.+$");
    private static final Pattern ARXIV_POST_2007 = Pattern.compile("(arxiv:)?(\\d{4})\\.(\\d{4,5})(v\\d+)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARXIV_POST_2007_WITH_CLASS = Pattern.compile("(arxiv:)?(?:[a-z\\-]+)(?:\\.[a-z]{2})?/(\\d{4})\\.(\\d{4,5})(v\\d+)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARXIV_PRE_2007 = Pattern.compile("(arxiv:)?([a-z\\-]+)(\\.[a-z]{2})?(/\\d{4})(\\d+)(v\\d+)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PMID = Pattern.compile("(pmid:|https?://pubmed.ncbi.nlm.nih.gov/)?(\\d+)/?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADS = Pattern.compile("(ads:|ADS:)?(\\d{4}[A-Za-z]\\S{13}[A-Za-z.:])$");
    private static final Pattern GND = Pattern.compile("(gnd:|GND:|https?://d-nb\\.info/gnd/|d-nb\\.info/gnd/)?(1[012]?\\d{7}[0-9X]|[47]\\d{6}-\\d|[1-9]\\d{0,7}-[0-9X]|3\\d{7}[0-9X])");
    private static final Pattern ROR = Pattern.compile("(?:https?://)?(?:ror\\.org/)?(0\\w{6}\\d{2})$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSTR = Pattern.compile("(?i)^(?:cstr:)?(?:[A-Za-z0-9_-]+\\.){3}[A-Za-z0-9_-]+$");
    private static final Pattern RRID = Pattern.compile("(?i)^(?:rrid:)?(AB|CVCL|SCR|IMSR|Addgene|DGRC|Flybase|NCBITaxon|MGI|BDSC|WB-STRAIN|ZFIN|ZIRC|DGGR|IMSR_GPT|IMSR_CYAGEN|IMSR_EUMMCR|IMSR_JAX|IMSR_RIKEN_BRC|IMSR_MMRRC|IMSR_TIGM|IMSR_CRL|IMSR_KU|ISMR_NM|ISMR_EM)+_[A-Za-z0-9_-]+$");
    private static final Pattern WIKIDATA = Pattern.compile("(wikidata:\\s*|(?:https?://)?www\\.wikidata\\.org/entity/)?(Q\\d+$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LSID = Pattern.compile("urn:lsid:[^:]+(:[^:]+){2,3}$", Pattern.CASE_INSENSITIVE);

    private static final List<String> ORCID_URLS = List.of("http://orcid.org/", "https://orcid.org/");
    private static final List<String> ISNI_URLS = List.of("http://isni.org/isni/", "https://isni.org/isni/");
    private static final long[][] ORCID_ISNI_RANGES = {{15_000_000L, 35_000_000L}, {900_000_000_000L, 900_100_000_000L}};
    private static final Set<String> PURL_HOSTS = Set.of("purl.org", "purl.oclc.org", "purl.net", "purl.com", "purl.fdlp.gov");

    private static final Predicate<String> ALWAYS_VALID = value -> true;

    /** The identifier schemes of people and organisations, with Zenodo's EDMO. */
    public static final Map<String, Predicate<String>> PERSON_OR_ORG_SCHEMES = Map.of(
            "orcid", ZenodoIdentifiers::isOrcid,
            "isni", ZenodoIdentifiers::isIsni,
            "gnd", ZenodoIdentifiers::isGnd,
            "ror", ZenodoIdentifiers::isRor,
            "edmo", ALWAYS_VALID);

    /** The schemes of alternate and related identifiers. */
    public static final Map<String, Predicate<String>> IDENTIFIER_SCHEMES = Map.ofEntries(
            Map.entry("ark", ZenodoIdentifiers::isArk),
            Map.entry("arxiv", ZenodoIdentifiers::isArxiv),
            Map.entry("ads", value -> ADS.matcher(java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKD)).lookingAt()),
            Map.entry("crossreffunderid", ALWAYS_VALID),
            Map.entry("cstr", value -> CSTR.matcher(value).lookingAt()),
            Map.entry("doi", ZenodoIdentifiers::isDoi),
            Map.entry("ean13", ZenodoIdentifiers::isEan13),
            Map.entry("eissn", ZenodoIdentifiers::isIssn),
            Map.entry("grid", ALWAYS_VALID),
            Map.entry("handle", value -> HANDLE.matcher(value).lookingAt()),
            Map.entry("igsn", ALWAYS_VALID),
            Map.entry("isbn", ZenodoIdentifiers::isIsbn),
            Map.entry("isni", ZenodoIdentifiers::isIsni),
            Map.entry("issn", ZenodoIdentifiers::isIssn),
            Map.entry("istc", ZenodoIdentifiers::isIstc),
            Map.entry("lissn", ZenodoIdentifiers::isIssn),
            Map.entry("lsid", value -> isUrn(value) && LSID.matcher(value).lookingAt()),
            Map.entry("pmid", value -> PMID.matcher(value).lookingAt()),
            Map.entry("purl", ZenodoIdentifiers::isPurl),
            Map.entry("rrid", value -> RRID.matcher(value).lookingAt()),
            Map.entry("upc", ALWAYS_VALID),
            Map.entry("url", ZenodoIdentifiers::isUrl),
            Map.entry("urn", ZenodoIdentifiers::isUrn),
            Map.entry("wikidata", value -> WIKIDATA.matcher(value).lookingAt()),
            Map.entry("w3id", ALWAYS_VALID),
            Map.entry("other", ALWAYS_VALID));

    private ZenodoIdentifiers() {
    }

    public static boolean isOrcid(String value) {
        for (String isniUrl : ISNI_URLS) if (value.startsWith(isniUrl)) return false;
        String id = value;
        for (String orcidUrl : ORCID_URLS) {
            if (id.startsWith(orcidUrl)) {
                id = id.substring(orcidUrl.length());
                break;
            }
        }
        id = id.replace("-", "").replace(" ", "");
        if (!isIsni(id)) return false;
        long number = Long.parseLong(id.substring(0, id.length() - 1));
        for (long[] range : ORCID_ISNI_RANGES) if (number >= range[0] && number <= range[1]) return true;
        return false;
    }

    public static boolean isIsni(String value) {
        String id = value;
        for (String isniUrl : ISNI_URLS) {
            if (id.startsWith(isniUrl)) {
                id = id.substring(isniUrl.length());
                break;
            }
        }
        id = id.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
        if (id.length() != 16) return false;
        int r = 0;
        for (int i = 0; i < 15; i++) {
            if (!Character.isDigit(id.charAt(i))) return false;
            r = (r + (id.charAt(i) - '0')) * 2;
        }
        int check = (12 - r % 11) % 11;
        char last = id.charAt(15);
        return last == 'X' ? check == 10 : Character.isDigit(last) && check == last - '0';
    }

    public static boolean isGnd(String value) {
        return GND.matcher(value).lookingAt();
    }

    public static boolean isRor(String value) {
        return ROR.matcher(value).lookingAt();
    }

    public static boolean isDoi(String value) {
        return DOI.matcher(value).lookingAt();
    }

    public static boolean isArk(String value) {
        if (ARK_SUFFIX.matcher(value).lookingAt()) return true;
        try {
            URI uri = new URI(value);
            return "http".equals(uri.getScheme()) && uri.getHost() != null && uri.getPath() != null && uri.getPath().length() > 1
                    && ARK_SUFFIX.matcher(uri.getPath().substring(1)).lookingAt();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isArxiv(String value) {
        return ARXIV_POST_2007.matcher(value).lookingAt() || ARXIV_POST_2007_WITH_CLASS.matcher(value).lookingAt() || ARXIV_PRE_2007.matcher(value).lookingAt();
    }

    public static boolean isIssn(String value) {
        String id = value.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
        if (id.length() != 8) return false;
        int r = 0;
        for (int i = 0; i < 8; i++) {
            char c = id.charAt(i);
            int digit = c == 'X' ? 10 : Character.isDigit(c) ? c - '0' : -1;
            if (digit < 0) return false;
            r += (8 - i) * digit;
        }
        return r % 11 == 0;
    }

    public static boolean isIstc(String value) {
        String id = value.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
        if (id.length() != 16) return false;
        int[] sequence = {11, 9, 3, 1};
        int r = 0;
        for (int i = 0; i < 15; i++) {
            int digit = Character.digit(id.charAt(i), 16);
            if (digit < 0) return false;
            r += digit * sequence[i % 4];
        }
        return Character.toUpperCase(Character.forDigit(r % 16, 16)) == id.charAt(15);
    }

    public static boolean isEan13(String value) {
        if (value.length() != 13) return false;
        int r = 0;
        for (int i = 0; i < 12; i++) {
            if (!Character.isDigit(value.charAt(i))) return false;
            r += (value.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return Character.isDigit(value.charAt(12)) && (10 - r % 10) % 10 == value.charAt(12) - '0';
    }

    /** An ISBN-10 or ISBN-13; a 13-digit one outside the 978 and 979 prefixes only when it is not also an EAN-13. */
    public static boolean isIsbn(String value) {
        if (isIsbn10(value) || isIsbn13(value)) return value.startsWith("978") || value.startsWith("979") || !isEan13(value);
        return false;
    }

    private static boolean isIsbn10(String value) {
        String id = value.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
        if (id.length() != 10) return false;
        int r = 0;
        for (int i = 0; i < 10; i++) {
            char c = id.charAt(i);
            int digit = i == 9 && c == 'X' ? 10 : Character.isDigit(c) ? c - '0' : -1;
            if (digit < 0) return false;
            r += (10 - i) * digit;
        }
        return r % 11 == 0;
    }

    private static boolean isIsbn13(String value) {
        String id = value.replace("-", "").replace(" ", "");
        return (id.startsWith("978") || id.startsWith("979")) && isEan13(id);
    }

    public static boolean isUrl(String value) {
        try {
            URI uri = new URI(value.trim());
            return uri.getScheme() != null && uri.getRawAuthority() != null && !uri.getRawAuthority().isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isUrn(String value) {
        try {
            URI uri = new URI(value);
            return "urn".equalsIgnoreCase(uri.getScheme()) && uri.getRawAuthority() == null && uri.getRawSchemeSpecificPart() != null && !uri.getRawSchemeSpecificPart().isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isPurl(String value) {
        try {
            URI uri = new URI(value);
            return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) && uri.getHost() != null && PURL_HOSTS.contains(uri.getHost())
                    && uri.getPath() != null && !uri.getPath().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}

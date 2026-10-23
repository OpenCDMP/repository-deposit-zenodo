package org.opencdmp.deposit.zenodorepository.validation;

import gr.cite.tools.exception.MyValidationException;

import java.util.*;

/** Turns problems, the plugin's own or the "errors" Zenodo answers with, into one message named by the review's labels. */
public final class ZenodoErrors {

    /** Zenodo's field paths by the review label of the section they are in. */
    private static final Map<String, String> SECTIONS = Map.ofEntries(
            Map.entry("metadata.title", "Title"),
            Map.entry("metadata.description", "Description"),
            Map.entry("metadata.publication_date", "Publication date"),
            Map.entry("metadata.version", "Version"),
            Map.entry("metadata.publisher", "Publisher"),
            Map.entry("metadata.resource_type", "Resource type"),
            Map.entry("metadata.languages", "Languages"),
            Map.entry("metadata.subjects", "Keywords and subjects"),
            Map.entry("metadata.rights", "Licenses"),
            Map.entry("metadata.copyright", "Copyright"),
            Map.entry("metadata.references", "References"),
            Map.entry("metadata.related_identifiers", "Related works"),
            Map.entry("metadata.creators", "Creators"),
            Map.entry("metadata.contributors", "Contributors"),
            Map.entry("metadata.funding", "Funding"),
            Map.entry("metadata.additional_titles", "Additional titles"),
            Map.entry("metadata.additional_descriptions", "Additional descriptions"),
            Map.entry("metadata.dates", "Dates"),
            Map.entry("metadata.identifiers", "Alternate identifiers"),
            Map.entry("access.files", "File access"),
            Map.entry("access.record", "File access"),
            Map.entry("access.embargo", "Embargo until"),
            Map.entry("custom_fields.journal:journal", "Publishing information"),
            Map.entry("custom_fields.imprint:imprint", "Publishing information"),
            Map.entry("custom_fields.thesis:thesis", "Publishing information"),
            Map.entry("custom_fields.meeting:meeting", "Conference"),
            Map.entry("custom_fields.code:codeRepository", "Repository URL"),
            Map.entry("custom_fields.code:programmingLanguage", "Programming language"),
            Map.entry("custom_fields.code:developmentStatus", "Development status"));

    /** The sections the review shows as rows, so an index in the path is a row number. */
    private static final Set<String> ROW_SECTIONS = Set.of("Creators", "Contributors", "Funding", "Additional titles", "Additional descriptions", "Dates", "Alternate identifiers");

    private ZenodoErrors() {
    }

    /** The "errors" of a Zenodo draft or validation response: [{"field": "metadata.identifiers.0.identifier", "messages": [...]}]. */
    public static Map<String, List<String>> fromZenodo(Object errors) {
        Map<String, List<String>> problems = new LinkedHashMap<>();
        if (!(errors instanceof List<?> list)) return problems;
        for (Object error : list) {
            if (!(error instanceof Map<?, ?> entry)) continue;
            String field = entry.get("field") != null ? entry.get("field").toString() : null;
            String label = label(field);
            Object messages = entry.get("messages");
            if (messages instanceof List<?> messageList) {
                for (Object message : messageList) if (message != null) add(problems, label, flatten(message));
            } else if (messages != null) {
                add(problems, label, flatten(messages));
            }
        }
        return problems;
    }

    public static MyValidationException exception(String intro, Map<String, List<String>> problems) {
        StringBuilder message = new StringBuilder(intro);
        List<Map.Entry<String, List<String>>> errors = new ArrayList<>();
        for (Map.Entry<String, List<String>> problem : problems.entrySet()) {
            for (String text : problem.getValue()) message.append(' ').append(problem.getKey()).append(": ").append(text.endsWith(".") ? text : text + ".");
            errors.add(Map.entry(problem.getKey(), problem.getValue()));
        }
        return new MyValidationException(message.toString(), errors);
    }

    static String label(String field) {
        if (field == null || field.isBlank()) return "Record";
        String section = null;
        String rest = "";
        for (Map.Entry<String, String> entry : SECTIONS.entrySet()) {
            String path = entry.getKey();
            if ((field.equals(path) || field.startsWith(path + ".")) && (section == null || path.length() > rest.length())) {
                section = entry.getValue();
                rest = path;
            }
        }
        if (section == null) return field;
        String remainder = field.length() > rest.length() ? field.substring(rest.length() + 1) : "";
        String index = remainder.split("\\.", 2)[0];
        if (ROW_SECTIONS.contains(section) && index.matches("\\d+")) return section + ", row " + (Integer.parseInt(index) + 1);
        return section;
    }

    private static String flatten(Object message) {
        if (message instanceof Map<?, ?> map) {
            // nested errors come as {"field": ["message"]}
            List<String> parts = new ArrayList<>();
            map.forEach((key, value) -> parts.add(key + ": " + (value instanceof List<?> l ? String.join(" ", l.stream().map(String::valueOf).toList()) : String.valueOf(value))));
            return String.join("; ", parts);
        }
        return String.valueOf(message);
    }

    private static void add(Map<String, List<String>> problems, String label, String message) {
        List<String> messages = problems.computeIfAbsent(label, k -> new ArrayList<>());
        if (!messages.contains(message)) messages.add(message);
    }
}

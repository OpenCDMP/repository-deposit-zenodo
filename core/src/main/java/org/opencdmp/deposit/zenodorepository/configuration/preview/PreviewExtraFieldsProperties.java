package org.opencdmp.deposit.zenodorepository.configuration.preview;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Deployment-specific extra custom fields to expose in the deposit preview as
 * Manual fields (e.g. Zenodo domain fields such as dwc:*). InvenioRDM exposes no
 * API for its custom-field schema, so the deployment declares here the keys its
 * target instance actually registers; a key the instance does not know is
 * silently dropped by Zenodo, never an error.
 */
@ConfigurationProperties(prefix = "preview-extra-fields")
public class PreviewExtraFieldsProperties {

    private List<ExtraField> fields = new ArrayList<>();

    public List<ExtraField> getFields() {
        return fields;
    }

    public void setFields(List<ExtraField> fields) {
        this.fields = fields;
    }

    public static class ExtraField {

        /** The custom_fields key, e.g. "dwc:family". Also used as the preview field code. */
        private String key;

        private String label;

        /** One of: text, textarea, tags, boolean, number, date. Defaults to text. */
        private String type;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}

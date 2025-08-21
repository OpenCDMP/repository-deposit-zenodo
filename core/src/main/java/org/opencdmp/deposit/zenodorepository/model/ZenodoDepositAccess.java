package org.opencdmp.deposit.zenodorepository.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.opencdmp.deposit.zenodorepository.enums.ZenodoAccessRight;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ZenodoDepositAccess {

    private ZenodoAccessRight record;

    private ZenodoAccessRight files;

    private Embargo embargo;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Embargo {
        private boolean active;
        private String until;
        private String reason;

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public String getUntil() {
            return until;
        }

        public void setUntil(String until) {
            this.until = until;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public ZenodoAccessRight getRecord() {
        return record;
    }

    public void setRecord(ZenodoAccessRight record) {
        this.record = record;
    }

    public ZenodoAccessRight getFiles() {
        return files;
    }

    public void setFiles(ZenodoAccessRight files) {
        this.files = files;
    }

    public Embargo getEmbargo() {
        return embargo;
    }

    public void setEmbargo(Embargo embargo) {
        this.embargo = embargo;
    }
}

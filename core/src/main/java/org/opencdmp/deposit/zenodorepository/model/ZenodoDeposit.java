package org.opencdmp.deposit.zenodorepository.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ZenodoDeposit {

    private ZenodoDepositAccess access;

    private ZenodoDepositMetadata metadata;

    @JsonProperty("custom_fields")
    private ZenodoDepositCustomFields customFields;

    public ZenodoDepositAccess getAccess() {
        return access;
    }

    public void setAccess(ZenodoDepositAccess access) {
        this.access = access;
    }

    public ZenodoDepositMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ZenodoDepositMetadata metadata) {
        this.metadata = metadata;
    }

    public ZenodoDepositCustomFields getCustomFields() {
        return customFields;
    }

    public void setCustomFields(ZenodoDepositCustomFields customFields) {
        this.customFields = customFields;
    }
}

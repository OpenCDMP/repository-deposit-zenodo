package org.opencdmp.deposit.zenodorepository.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ZenodoDeposit {

    private ZenodoDepositMetadata metadata;

    public ZenodoDepositMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ZenodoDepositMetadata metadata) {
        this.metadata = metadata;
    }
}

package org.opencdmp.deposit.zenodorepository.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import org.opencdmp.commonmodels.enums.EnumUtils;
import org.opencdmp.commonmodels.enums.EnumValueProvider;

import java.util.Map;

public enum ZenodoAccessRight implements EnumValueProvider<String> {
    RESTRICTED(Names.Restricted), PUBLIC(Names.Public);

    private final String value;

    public static class Names {
        public static final String Restricted = "restricted";
        public static final String Public = "public";
    }

    ZenodoAccessRight(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String getValue() {
        return value;
    }

    private static final Map<String, ZenodoAccessRight> map = EnumUtils.getEnumValueMap(ZenodoAccessRight.class);

    public static ZenodoAccessRight of(String i) {
        return map.get(i);
    }
}

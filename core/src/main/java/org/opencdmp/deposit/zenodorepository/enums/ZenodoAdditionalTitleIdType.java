package org.opencdmp.deposit.zenodorepository.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import org.opencdmp.commonmodels.enums.EnumUtils;
import org.opencdmp.commonmodels.enums.EnumValueProvider;

import java.util.Map;

public enum ZenodoAdditionalTitleIdType implements EnumValueProvider<String> {
    ALTERNATIVE_TITLE(Names.AlternativeTitle),
    SUBTITLE(Names.Subtitle),
    TRANSLATED_TITLE(Names.TranslatedTitle),
    OTHER(Names.Other);

    private final String value;

    public static class Names {
        public static final String AlternativeTitle = "alternative-title";
        public static final String Subtitle = "subtitle";
        public static final String TranslatedTitle = "translated-title";
        public static final String Other = "other";
    }

    ZenodoAdditionalTitleIdType(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String getValue() {
        return value;
    }

    private static final Map<String, ZenodoAdditionalTitleIdType> map = EnumUtils.getEnumValueMap(ZenodoAdditionalTitleIdType.class);

    public static ZenodoAdditionalTitleIdType of(String i) {
        return map.get(i);
    }
}

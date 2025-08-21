package org.opencdmp.deposit.zenodorepository.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.opencdmp.deposit.zenodorepository.enums.ZenodoAdditionalTitleIdType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ZenodoAdditionalTitle {

    private String title;

    private Type type;

    private Language lang;

    public static class Type {
        private ZenodoAdditionalTitleIdType id;

        public ZenodoAdditionalTitleIdType getId() {
            return id;
        }

        public void setId(ZenodoAdditionalTitleIdType id) {
            this.id = id;
        }

    }

    public static class Language {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Language getLang() {
        return lang;
    }

    public void setLang(Language lang) {
        this.lang = lang;
    }
}

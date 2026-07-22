package com.apiautomation.ai;

import java.util.Set;

public class AiMutation {
    public static final Set<String> ALLOWED_TYPES = Set.of(
            "OMIT_FIELD",
            "REPLACE_FIELD",
            "OMIT_BODY",
            "OMIT_HEADER",
            "SET_CONTENT_TYPE",
            "WRONG_METHOD",
            "INVALID_JSON"
    );

    private String type;
    private String field;
    private Object value;
    private String header;
    private String method;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}

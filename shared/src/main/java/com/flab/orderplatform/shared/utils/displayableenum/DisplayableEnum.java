package com.flab.orderplatform.shared.utils.displayableenum;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = DisplayableEnumSerializer.class)
@JsonDeserialize(using = DisplayableEnumDeserializer.class)
public interface DisplayableEnum {
    String getDescription();
    default String getCode() {
        return ((Enum<?>) this).name();
    }
}

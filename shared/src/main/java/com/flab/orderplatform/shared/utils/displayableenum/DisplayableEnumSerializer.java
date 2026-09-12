package com.flab.orderplatform.shared.utils.displayableenum;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Enum 직렬화
 */
public class DisplayableEnumSerializer extends JsonSerializer<DisplayableEnum> {
    @Override
    public void serialize(DisplayableEnum value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("code", value.getCode());
        jsonGenerator.writeStringField("description", value.getDescription());
        jsonGenerator.writeEndObject();
    }
}

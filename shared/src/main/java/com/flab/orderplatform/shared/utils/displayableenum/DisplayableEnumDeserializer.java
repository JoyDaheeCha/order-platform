package com.flab.orderplatform.shared.utils.displayableenum;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

import java.io.IOException;

public class DisplayableEnumDeserializer extends JsonDeserializer<Enum<?>> implements ContextualDeserializer {

    private Class<? extends Enum<?>> targetType;

    @Override
    @SuppressWarnings("unchecked")
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        var contextType = property != null ? property.getType() : ctxt.getContextualType();
        var deserializer = new DisplayableEnumDeserializer();
        deserializer.targetType = (Class<? extends Enum<?>>) contextType.getRawClass();
        return deserializer;
    }

    @Override
    public Enum<?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        var code = node.get("code").asText();
        return Enum.valueOf((Class) targetType, code);
    }
}

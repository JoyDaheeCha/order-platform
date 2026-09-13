package com.flab.orderplatform.shared.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class JsonUtils {
    private static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper();

        var javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        javaTimeModule.addSerializer(ZonedDateTime.class,
                new ZonedDateTimeSerializer(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        javaTimeModule.addDeserializer(ZonedDateTime.class, InstantDeserializer.ZONED_DATE_TIME);

        MAPPER.registerModules(
                javaTimeModule
        );

        MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public static String toJson(final Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (IOException e) {
            throw new JsonEncodeException(e);
        }
    }

    public static <T> T fromJsonFile(final String filePath, Class<T> valueType) {
        try (var is = JsonUtils.class.getClassLoader().getResourceAsStream(filePath)) {
            if (is == null) {
                throw new JsonDecodeException(new FileNotFoundException(filePath));
            }
            return MAPPER.readValue(is, valueType);
        } catch (IOException e) {
            throw new JsonDecodeException(e);
        }
    }

    public static <T> T fromJson(final String json, Class<T> valueType) {
        try {
            return MAPPER.readValue(json, valueType);
        } catch (IOException e) {
            throw new JsonDecodeException(e);
        }
    }

    public static class JsonEncodeException extends RuntimeException {
        public JsonEncodeException(Throwable cause) {
            super(cause);
        }
    }

    public static class JsonDecodeException extends RuntimeException {
        public JsonDecodeException(Throwable cause) {
            super(cause);
        }
    }
}

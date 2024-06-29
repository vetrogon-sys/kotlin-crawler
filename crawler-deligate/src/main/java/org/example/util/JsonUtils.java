package org.example.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.io.InputStream;

public final class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static JsonNode parseJson(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (IOException e) {
            return new JsonNodeFactory(false).missingNode();
        }
    }


}

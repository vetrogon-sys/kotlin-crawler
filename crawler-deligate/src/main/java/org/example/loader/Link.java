package org.example.loader;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Getter
public class Link {

    public static final Link EMPTY_URL = new Link.Builder("https://example.org/").build();

    @Setter
    private String url;
    private final Map<String, String> headers;
    private final Map<String, String> parameters;
    private final Map<String, Cookie> cookies;
    private String body;
    private HttpMethod method;

    public Link() {
        headers = new HashMap<>();
        parameters = new HashMap<>();
        cookies = new HashMap<>();
        method = HttpMethod.GET;
    }

    public Link addHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public Link addParameter(String key, String value) {
        parameters.put(key, value);
        return this;
    }

    public String getMethod() {
        return method.getName();
    }


    public static class Builder {
        private final String url;

        public Builder(String url) {
            this.url = url;
        }

        public Link build() {

            Link link = new Link();
            link.setUrl(url);
            return link;
        }

    }

    public String buildUrl() {
        if (parameters.isEmpty()) {
            return url;
        }

        StringBuilder paramsString = new StringBuilder();
        parameters.forEach((k, v) -> {
            paramsString.append(k).append('=').append(v).append('&');
        });

        return url + '?' + URLEncoder.encode(paramsString.substring(0, paramsString.length() - 1), StandardCharsets.UTF_8);
    }

    public record Cookie(String key, String value) {
    }

    ;

    @Getter
    public enum HttpMethod {
        GET("GET"),
        POST("POST");

        private final String name;

        HttpMethod(String name) {
            this.name = name;
        }
    }

}

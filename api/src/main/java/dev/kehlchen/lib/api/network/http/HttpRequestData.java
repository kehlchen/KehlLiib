package dev.kehlchen.lib.api.network.http;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record HttpRequestData(HttpMethod method, URI uri, Map<String, String> headers,
                             byte[] body, Duration timeout, RetryPolicy retryPolicy) {
    public HttpRequestData {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(uri, "uri");
        Objects.requireNonNull(timeout, "timeout");
        Objects.requireNonNull(retryPolicy, "retryPolicy");
        headers = normalizeHeaders(headers);
        body = body == null ? new byte[0] : body.clone();
        if (!uri.isAbsolute() || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("An absolute HTTP URI without embedded credentials is required");
        }
        if (timeout.compareTo(Duration.ofMillis(1)) < 0 || timeout.compareTo(Duration.ofMinutes(5)) > 0) {
            throw new IllegalArgumentException("timeout must be between 1 ms and 5 minutes");
        }
    }

    @Override public byte[] body() { return body.clone(); }

    @Override public String toString() {
        return "HttpRequestData[method=" + method + ", bodyBytes=" + body.length + "]";
    }

    public static Builder builder(HttpMethod method, URI uri) { return new Builder(method, uri); }
    public static Builder get(URI uri) { return builder(HttpMethod.GET, uri); }

    public static final class Builder {
        private final HttpMethod method;
        private URI uri;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private byte[] body = new byte[0];
        private Duration timeout = Duration.ofSeconds(15);
        private RetryPolicy retryPolicy = RetryPolicy.none();

        private Builder(HttpMethod method, URI uri) {
            this.method = Objects.requireNonNull(method, "method");
            this.uri = Objects.requireNonNull(uri, "uri");
        }

        public Builder uri(URI value) { uri = Objects.requireNonNull(value, "uri"); return this; }
        public Builder header(String name, String value) {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(value, "value");
            if (name.isBlank() || name.indexOf('\r') >= 0 || name.indexOf('\n') >= 0
                    || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
                throw new IllegalArgumentException("Invalid HTTP header");
            }
            headers.keySet().removeIf(existing -> existing.equalsIgnoreCase(name));
            headers.put(name, value);
            return this;
        }
        public Builder query(String name, String value) {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(value, "value");
            String encoded = java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8)
                    + "=" + java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
            String source = uri.toASCIIString();
            int fragmentIndex = source.indexOf('#');
            String fragment = fragmentIndex < 0 ? "" : source.substring(fragmentIndex);
            String base = fragmentIndex < 0 ? source : source.substring(0, fragmentIndex);
            String separator = uri.getRawQuery() == null || uri.getRawQuery().isEmpty() ? "?" : "&";
            uri = URI.create(base + separator
                    + encoded + fragment);
            return this;
        }
        public Builder body(byte[] value) { body = Objects.requireNonNull(value, "body").clone(); return this; }
        public Builder text(String value) {
            body = Objects.requireNonNull(value, "body").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return this;
        }
        public Builder json(String value) {
            header("Content-Type", "application/json; charset=utf-8");
            return text(value);
        }
        public Builder timeout(Duration value) { timeout = Objects.requireNonNull(value, "timeout"); return this; }
        public Builder retry(RetryPolicy value) { retryPolicy = Objects.requireNonNull(value, "retryPolicy"); return this; }
        public HttpRequestData build() { return new HttpRequestData(method, uri, headers, body, timeout, retryPolicy); }
    }

    private static Map<String, String> normalizeHeaders(Map<String, String> values) {
        LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
        values.forEach((name, value) -> {
            Objects.requireNonNull(name, "header name");
            Objects.requireNonNull(value, "header value");
            if (name.isBlank() || name.indexOf('\r') >= 0 || name.indexOf('\n') >= 0
                    || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
                throw new IllegalArgumentException("Invalid HTTP header");
            }
            normalized.keySet().removeIf(existing -> existing.equalsIgnoreCase(name));
            normalized.put(name, value);
        });
        return Map.copyOf(normalized);
    }
}

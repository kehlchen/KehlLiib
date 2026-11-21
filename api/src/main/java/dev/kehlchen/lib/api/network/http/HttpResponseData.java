package dev.kehlchen.lib.api.network.http;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public record HttpResponseData(int statusCode, Map<String, List<String>> headers, byte[] body) {
    public HttpResponseData {
        headers = headers.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
        body = body.clone();
    }

    @Override public byte[] body() { return body.clone(); }
    public boolean isSuccessful() { return statusCode >= 200 && statusCode < 300; }
    public String text() { return text(StandardCharsets.UTF_8); }
    public String text(Charset charset) { return new String(body, charset); }
    @Override public String toString() {
        return "HttpResponseData[statusCode=" + statusCode + ", bodyBytes=" + body.length + "]";
    }
    public List<String> header(String name) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst().orElse(List.of());
    }
}

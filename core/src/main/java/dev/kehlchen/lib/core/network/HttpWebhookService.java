package dev.kehlchen.lib.core.network;

import dev.kehlchen.lib.api.network.http.HttpClientService;
import dev.kehlchen.lib.api.network.http.HttpMethod;
import dev.kehlchen.lib.api.network.http.HttpRequestData;
import dev.kehlchen.lib.api.network.http.HttpCall;
import dev.kehlchen.lib.api.network.webhook.WebhookEmbed;
import dev.kehlchen.lib.api.network.webhook.WebhookMessage;
import dev.kehlchen.lib.api.network.webhook.WebhookService;
import dev.kehlchen.lib.core.serialization.JsonSerialization;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Generic webhook sender. Discord payload support is encoded here without coupling the HTTP API to Discord. */
public final class HttpWebhookService implements WebhookService {
    private final HttpClientService http;
    private final JsonSerialization json;

    public HttpWebhookService(HttpClientService http, JsonSerialization json) {
        this.http = Objects.requireNonNull(http, "http");
        this.json = Objects.requireNonNull(json, "json");
    }

    @Override public HttpCall send(URI endpoint, WebhookMessage message) {
        Objects.requireNonNull(message, "message");
        Map<String, Object> payload = new LinkedHashMap<>();
        put(payload, "content", message.content());
        put(payload, "username", message.username());
        put(payload, "avatar_url", message.avatarUrl());
        if (!message.embeds().isEmpty()) payload.put("embeds", message.embeds().stream().map(this::embed).toList());
        if (!message.components().isEmpty()) payload.put("components", message.components());
        HttpRequestData.Builder request = HttpRequestData.builder(HttpMethod.POST, endpoint)
                .json(json.toJson(payload)).timeout(message.timeout()).retry(message.retryPolicy());
        message.headers().forEach(request::header);
        return http.execute(request.build());
    }

    private Map<String, Object> embed(WebhookEmbed embed) {
        Map<String, Object> value = new LinkedHashMap<>();
        put(value, "title", embed.title());
        put(value, "description", embed.description());
        put(value, "url", embed.url());
        put(value, "color", embed.color());
        if (!embed.fields().isEmpty()) {
            List<Map<String, Object>> fields = new ArrayList<>();
            for (WebhookEmbed.Field field : embed.fields()) {
                fields.add(Map.of("name", field.name(), "value", field.value(), "inline", field.inline()));
            }
            value.put("fields", fields);
        }
        if (embed.footer() != null) {
            Map<String, Object> footer = new LinkedHashMap<>();
            footer.put("text", embed.footer().text());
            put(footer, "icon_url", embed.footer().iconUrl());
            value.put("footer", footer);
        }
        return value;
    }

    private static void put(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }
}

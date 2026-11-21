package dev.kehlchen.lib.api.network.webhook;

import dev.kehlchen.lib.api.network.http.RetryPolicy;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public record WebhookMessage(String content, String username, String avatarUrl,
                             List<WebhookEmbed> embeds, List<Map<String, Object>> components,
                             Map<String, String> headers, Duration timeout, RetryPolicy retryPolicy) {
    public WebhookMessage {
        embeds = embeds == null ? List.of() : List.copyOf(embeds);
        components = components == null ? List.of() : components.stream().map(Map::copyOf).toList();
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        timeout = timeout == null ? Duration.ofSeconds(10) : timeout;
        retryPolicy = retryPolicy == null ? RetryPolicy.none() : retryPolicy;
        if (timeout.isNegative() || timeout.isZero()) throw new IllegalArgumentException("timeout must be positive");
        if (timeout.compareTo(Duration.ofMinutes(5)) > 0) throw new IllegalArgumentException("timeout cannot exceed five minutes");
        if (content != null && content.length() > 2_000) throw new IllegalArgumentException("Webhook content exceeds 2000 characters");
        if (username != null && username.length() > 80) throw new IllegalArgumentException("Webhook username exceeds 80 characters");
        if (avatarUrl != null && avatarUrl.length() > 2_000) throw new IllegalArgumentException("Webhook avatar URL is too long");
        headers.forEach((name, value) -> {
            if (name.isBlank() || name.indexOf('\r') >= 0 || name.indexOf('\n') >= 0
                    || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
                throw new IllegalArgumentException("Invalid webhook header");
            }
        });
        if (embeds.size() > 10) throw new IllegalArgumentException("A webhook message may contain at most 10 embeds");
        if (components.size() > 5) throw new IllegalArgumentException("A webhook message may contain at most 5 component rows");
        if ((content == null || content.isBlank()) && embeds.isEmpty() && components.isEmpty()) {
            throw new IllegalArgumentException("A webhook message must contain content, embeds or components");
        }
        int embedCharacters = embeds.stream().mapToInt(WebhookMessage::characterCount).sum();
        if (embedCharacters > 6_000) throw new IllegalArgumentException("Webhook embeds exceed 6000 total characters");
    }

    public static WebhookMessage text(String content) {
        return new WebhookMessage(content, null, null, List.of(), List.of(), Map.of(),
                Duration.ofSeconds(10), RetryPolicy.none());
    }

    @Override public String toString() {
        return "WebhookMessage[embeds=" + embeds.size() + ", components=" + components.size()
                + ", headers=" + headers.size() + "]";
    }

    private static int characterCount(WebhookEmbed embed) {
        int count = length(embed.title()) + length(embed.description());
        count += embed.fields().stream().mapToInt(field -> field.name().length() + field.value().length()).sum();
        return count + (embed.footer() == null ? 0 : embed.footer().text().length());
    }

    private static int length(String value) { return value == null ? 0 : value.length(); }
}

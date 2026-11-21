package dev.kehlchen.lib.api.network.webhook;

import java.util.List;

public record WebhookEmbed(String title, String description, String url, Integer color,
                           List<Field> fields, Footer footer) {
    public WebhookEmbed {
        fields = fields == null ? List.of() : List.copyOf(fields);
        if (fields.size() > 25) throw new IllegalArgumentException("An embed may contain at most 25 fields");
        if (title != null && title.length() > 256) throw new IllegalArgumentException("Embed title exceeds 256 characters");
        if (description != null && description.length() > 4_096) {
            throw new IllegalArgumentException("Embed description exceeds 4096 characters");
        }
        if (url != null && url.length() > 2_000) throw new IllegalArgumentException("Embed URL is too long");
        if (color != null && (color < 0 || color > 0xFFFFFF)) throw new IllegalArgumentException("Embed color is out of range");
    }

    public record Field(String name, String value, boolean inline) {
        public Field {
            if (name == null || name.isBlank() || value == null || value.isBlank()) {
                throw new IllegalArgumentException("Embed field name and value are required");
            }
            if (name.length() > 256 || value.length() > 1_024) {
                throw new IllegalArgumentException("Embed field exceeds Discord length limits");
            }
        }
    }

    public record Footer(String text, String iconUrl) {
        public Footer {
            if (text == null || text.isBlank()) throw new IllegalArgumentException("Footer text is required");
            if (text.length() > 2_048 || iconUrl != null && iconUrl.length() > 2_000) {
                throw new IllegalArgumentException("Embed footer exceeds Discord length limits");
            }
        }
    }
}

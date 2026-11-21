package dev.kehlchen.lib.api.network.webhook;

import dev.kehlchen.lib.api.network.http.HttpCall;

import java.net.URI;

public interface WebhookService {
    HttpCall send(URI endpoint, WebhookMessage message);
}

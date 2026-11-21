# Networking

## HTTP

`HttpClientService` executes `HttpRequestData` using `HttpCall`. `HttpMethod` supports GET, POST, PUT, PATCH, and DELETE.

```java
HttpClientService http =
    runtime.services().getRequired(HttpClientService.class);

HttpCall call = http.execute(
    HttpRequestData.builder(HttpMethod.GET, URI.create("https://example.net/status"))
        .timeout(Duration.ofSeconds(5))
        .retry(RetryPolicy.none())
        .build());

call.thenAccept(result -> result.value().ifPresent(response -> {
    // This completion executes asynchronously, not on the server thread.
}));
```

Requests support headers, query parameters, text/JSON bodies, timeout, and bounded retry. `HttpResponseData` retains status, headers, and body. Non-2xx responses are ordinary HTTP responses, not transport exceptions.

The default client limits bodies to 2 MiB, concurrency to 32, rate to 20 requests per second, pending queue to 128, and disables redirects. HTTPS is required unless private/plain HTTP hosts are explicitly allow-listed.

Do not accept arbitrary player-controlled URLs or log secrets.

## WebSockets

`WebSocketService` connects to `wss://` endpoints and accepts a `WebSocketListener`. `WebSocketConnection` sends text or binary data; `WebSocketStatus` reports state.

WebSocket callbacks run on network threads. Schedule Bukkit work through `Scheduler` with appropriate entity/location ownership. Connections and workers are lifecycle-managed and closed during plugin shutdown.

## Webhooks

`WebhookService` sends `WebhookMessage` values to a webhook URL. `WebhookEmbed` supports title, description, color, fields, and footer.

Webhook sending is distinct from generic HTTP and does not guarantee provider-specific formatting. The URL may contain a secret and must not be logged.

## Plugin messaging

`PluginMessageService` uses a Bukkit player as the carrier. Register channels and payload codecs with `NamespacedKey` values. Payloads are size-bounded.

```java
PluginMessageService messages =
    runtime.services().getRequired(PluginMessageService.class);
NamespacedKey channel = new NamespacedKey(plugin, "sync");

messages.register(plugin, channel,
    (carrier, receivedChannel, bytes) ->
        handlePayload(player, bytes));

Result<Void> sent =
    messages.sendString(plugin, player, channel, "hello");
```

Plugin messaging does not provide server-to-server transport by itself. It requires an online carrier player and is not a general network channel.

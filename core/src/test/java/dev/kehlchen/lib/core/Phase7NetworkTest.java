package dev.kehlchen.lib.core;

import com.sun.net.httpserver.HttpServer;
import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.network.http.HttpClientConfig;
import dev.kehlchen.lib.api.network.http.HttpMethod;
import dev.kehlchen.lib.api.network.http.HttpRequestData;
import dev.kehlchen.lib.api.network.http.RetryPolicy;
import dev.kehlchen.lib.api.network.webhook.WebhookEmbed;
import dev.kehlchen.lib.api.network.webhook.WebhookMessage;
import dev.kehlchen.lib.core.network.AsyncHttpClient;
import dev.kehlchen.lib.core.network.HttpWebhookService;
import dev.kehlchen.lib.core.serialization.JsonSerialization;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class Phase7NetworkTest {
    private HttpServer server;
    private String baseUrl;
    private final AtomicInteger retryCount = new AtomicInteger();
    private volatile String capturedWebhook;
    private volatile String capturedMethod;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] request = exchange.getRequestBody().readAllBytes();
            capturedMethod = exchange.getRequestMethod();
            if (exchange.getRequestURI().getPath().equals("/retry") && retryCount.getAndIncrement() == 0) {
                exchange.sendResponseHeaders(503, -1);
                exchange.close();
                return;
            }
            if (exchange.getRequestURI().getPath().equals("/slow")) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    exchange.close();
                    return;
                }
            }
            if (exchange.getRequestURI().getPath().equals("/large")) {
                request = new byte[128];
            }
            capturedWebhook = new String(request, StandardCharsets.UTF_8);
            byte[] response = (exchange.getRequestMethod() + ":" + new String(request, StandardCharsets.UTF_8))
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("X-Test", "present");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void requestBuilderEncodesQueryAndKeepsFragmentAtEnd() {
        HttpRequestData request = HttpRequestData.get(URI.create("https://example.test/items?old=1#section"))
                .query("filter", "red blue&green")
                .header("Accept", "application/json")
                .build();

        assertEquals("https://example.test/items?old=1&filter=red+blue%26green#section",
                request.uri().toASCIIString());
        assertEquals("application/json", request.headers().get("Accept"));
        HttpRequestData secretRequest = HttpRequestData.get(URI.create("https://example.test/"))
                .header("Authorization", "Bearer do-not-log")
                .build();
        assertFalse(secretRequest.toString().contains("do-not-log"));
        assertThrows(IllegalArgumentException.class, () -> HttpRequestData.get(URI.create("https://example.test/"))
                .header("X-Test", "safe\r\nInjected: true"));
    }

    @Test
    void executesMethodsAndReturnsStatusHeadersAndBodyAsynchronously() throws Exception {
        HttpClientConfig config = localConfig(1024);
        AsyncHttpClient client = new AsyncHttpClient(config);
        try {
            for (HttpMethod method : HttpMethod.values()) {
                HttpRequestData.Builder builder = HttpRequestData.builder(method, URI.create(baseUrl + "/echo"));
                if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
                    builder.text("payload");
                }
                Result<dev.kehlchen.lib.api.network.http.HttpResponseData> result =
                        client.execute(builder.build()).get(3, TimeUnit.SECONDS);
                assertTrue(result.isSuccess());
                assertEquals(200, result.value().orElseThrow().statusCode());
                assertEquals(method.name() + ":" + ((method == HttpMethod.POST || method == HttpMethod.PUT
                        || method == HttpMethod.PATCH) ? "payload" : ""),
                        result.value().orElseThrow().text());
                assertEquals(List.of("present"), result.value().orElseThrow().header("X-Test"));
            }
        } finally { client.onDisable(); }
    }

    @Test
    void enforcesTimeoutAndResponseSizeAndSupportsBoundedRetry() throws Exception {
        AsyncHttpClient client = new AsyncHttpClient(localConfig(32));
        try {
            Result<?> timedOut = client.execute(HttpRequestData.get(URI.create(baseUrl + "/slow"))
                    .timeout(Duration.ofMillis(30)).build()).get(3, TimeUnit.SECONDS);
            assertFalse(timedOut.isSuccess());

            Result<?> oversized = client.execute(HttpRequestData.get(URI.create(baseUrl + "/large"))
                    .build()).get(3, TimeUnit.SECONDS);
            assertFalse(oversized.isSuccess());

            RetryPolicy retry = new RetryPolicy(2, Duration.ZERO, Duration.ZERO, Set.of(503));
            Result<dev.kehlchen.lib.api.network.http.HttpResponseData> retried = client.execute(
                    HttpRequestData.get(URI.create(baseUrl + "/retry")).retry(retry).build())
                    .get(3, TimeUnit.SECONDS);
            assertTrue(retried.isSuccess());
            assertEquals(200, retried.value().orElseThrow().statusCode());
            assertEquals(2, retryCount.get());
        } finally { client.onDisable(); }
    }

    @Test
    void cancelsRequestsAndRejectsUnsafeTargetsBeforeSending() throws Exception {
        AsyncHttpClient client = new AsyncHttpClient(localConfig(1024));
        try {
            var call = client.execute(HttpRequestData.get(URI.create(baseUrl + "/slow")).build());
            assertTrue(call.cancel(true));
            assertTrue(call.isCancelled());

            Result<?> localTarget = client.execute(HttpRequestData.get(URI.create("http://localhost:8080/"))
                    .build()).get(1, TimeUnit.SECONDS);
            assertFalse(localTarget.isSuccess());
        } finally { client.onDisable(); }

        AsyncHttpClient strictClient = new AsyncHttpClient(HttpClientConfig.defaults());
        try {
            Result<?> privateTarget = strictClient.execute(HttpRequestData.get(URI.create("https://127.0.0.1/"))
                    .build()).get(2, TimeUnit.SECONDS);
            assertFalse(privateTarget.isSuccess());
        } finally { strictClient.onDisable(); }
    }

    @Test
    void boundsQueuedRequests() throws Exception {
        AsyncHttpClient client = new AsyncHttpClient(new HttpClientConfig(Duration.ofSeconds(1),
                1, 1024, 1024, 1, 0, 100, true, Set.of("127.0.0.1")));
        try {
            var first = client.execute(HttpRequestData.get(URI.create(baseUrl + "/slow"))
                    .timeout(Duration.ofSeconds(2)).build());
            Result<?> overflow = client.execute(HttpRequestData.get(URI.create(baseUrl + "/echo"))
                    .build()).get(1, TimeUnit.SECONDS);
            assertFalse(overflow.isSuccess());
            first.cancel(true);
        } finally { client.onDisable(); }
    }

    @Test
    void discordWebhookSerializesJsonWithoutDiscordCouplingInHttpClient() throws Exception {
        AsyncHttpClient client = new AsyncHttpClient(localConfig(1024));
        try {
            HttpWebhookService webhooks = new HttpWebhookService(client, new JsonSerialization());
            WebhookMessage message = new WebhookMessage(null, "KehlLib", null,
                    List.of(new WebhookEmbed("title", "description", null, 0x123456,
                            List.of(new WebhookEmbed.Field("field", "value", true)),
                            new WebhookEmbed.Footer("footer", null))),
                    List.of(), Map.of("X-Webhook-Test", "yes"), Duration.ofSeconds(2), RetryPolicy.none());
            var result = webhooks.send(URI.create(baseUrl + "/webhook"), message).get(3, TimeUnit.SECONDS);

            assertTrue(result.isSuccess());
            assertEquals("POST", capturedMethod);
            assertTrue(capturedWebhook.contains("\"avatar_url\"") == false);
            assertTrue(capturedWebhook.contains("\"username\":\"KehlLib\""));
            assertTrue(capturedWebhook.contains("\"icon_url\"") == false);
            assertTrue(capturedWebhook.contains("\"inline\":true"));
            assertTrue(capturedWebhook.contains("\"color\":1193046"));
            assertFalse(message.toString().contains("yes"));
        } finally { client.onDisable(); }
    }

    private HttpClientConfig localConfig(int maximumResponseBytes) {
        return new HttpClientConfig(Duration.ofSeconds(1), 2, 1024,
                maximumResponseBytes, 4, 8, 100, true, Set.of("127.0.0.1"));
    }
}

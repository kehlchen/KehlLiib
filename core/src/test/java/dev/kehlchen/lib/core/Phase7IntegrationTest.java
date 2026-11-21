package dev.kehlchen.lib.core;

import dev.kehlchen.lib.api.Resource;
import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.economy.EconomyProvider;
import dev.kehlchen.lib.api.economy.EconomyTransaction;
import dev.kehlchen.lib.api.integration.Integration;
import dev.kehlchen.lib.api.integration.proxy.ProxyPlayerInfo;
import dev.kehlchen.lib.api.messaging.PayloadCodec;
import dev.kehlchen.lib.api.messaging.PluginMessagePayload;
import dev.kehlchen.lib.api.network.websocket.WebSocketConfig;
import dev.kehlchen.lib.api.network.websocket.WebSocketListener;
import dev.kehlchen.lib.core.integration.IntegrationRegistryImpl;
import dev.kehlchen.lib.core.network.JdkWebSocketService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class Phase7IntegrationTest {
    @Test
    void managesAvailableAndUnavailableIntegrationsAndClosesInReverseOrder() {
        List<String> calls = new ArrayList<>();
        IntegrationRegistryImpl registry = new IntegrationRegistryImpl();
        TestIntegration first = new TestIntegration("first", true, calls);
        TestIntegration unavailable = new TestIntegration("unavailable", false, calls);
        registry.register(first);
        registry.register(unavailable);
        registry.onEnable();

        assertEquals(List.of("enable:first"), calls);
        assertTrue(registry.find("first").isPresent());
        assertTrue(registry.unregister("unavailable"));

        TestIntegration dynamic = new TestIntegration("dynamic", true, calls);
        registry.register(dynamic);
        registry.unregister("first");
        assertEquals(List.of("enable:first", "enable:dynamic", "disable:first"), calls);
        registry.onDisable();
        assertEquals(List.of("enable:first", "enable:dynamic", "disable:first", "disable:dynamic"), calls);
    }

    @Test
    void boundsAndCopiesPluginMessagePayloadsAndSupportsCodecs() throws Exception {
        PayloadCodec<String> codec = new PayloadCodec<>() {
            @Override public byte[] encode(String value) { return value.getBytes(java.nio.charset.StandardCharsets.UTF_8); }
            @Override public String decode(byte[] payload) { return new String(payload, java.nio.charset.StandardCharsets.UTF_8); }
        };
        PluginMessagePayload payload = PluginMessagePayload.encode("naïve", codec);
        byte[] exposed = payload.bytes();
        exposed[0] = 0;

        assertEquals("naïve", payload.decode(codec));
        assertEquals("naïve", PluginMessagePayload.utf8("naïve").utf8());
        assertThrows(IllegalArgumentException.class,
                () -> PluginMessagePayload.of(new byte[]{(byte) 0xC3}).utf8());
        assertThrows(IllegalArgumentException.class,
                () -> PluginMessagePayload.of(new byte[PluginMessagePayload.MAX_BYTES + 1]));
    }

    @Test
    void rejectsInsecureWebSocketUrisWithoutOpeningNetworkConnections() throws Exception {
        JdkWebSocketService service = new JdkWebSocketService();
        try {
            var result = service.connect(URI.create("ws://example.invalid"), WebSocketConfig.defaults(),
                    new WebSocketListener() {}).get(1, TimeUnit.SECONDS);
            assertFalse(result.isSuccess());
        } finally {
            service.onDisable();
        }
    }

    @Test
    void economyTransactionHasExplicitSuccessAndFailureDetails() {
        EconomyTransaction success = new EconomyTransaction(true, new BigDecimal("2.50"),
                new BigDecimal("12.50"), null);
        EconomyTransaction failure = new EconomyTransaction(false, new BigDecimal("2.50"),
                null, "insufficient funds");

        assertTrue(success.resultingBalance().isPresent());
        assertTrue(success.failureReason().isEmpty());
        assertTrue(failure.resultingBalance().isEmpty());
        assertEquals("insufficient funds", failure.failureReason().orElseThrow());
    }

    private static final class TestIntegration implements Integration {
        private final String name;
        private final boolean available;
        private final List<String> calls;
        private TestIntegration(String name, boolean available, List<String> calls) {
            this.name = name;
            this.available = available;
            this.calls = calls;
        }
        @Override public String name() { return name; }
        @Override public String version() { return "test"; }
        @Override public boolean isAvailable() { return available; }
        @Override public void onEnable() { calls.add("enable:" + name); }
        @Override public void onDisable() { calls.add("disable:" + name); }
    }
}

package dev.kehlchen.lib.integration.redis;

import dev.kehlchen.lib.api.Resource;
import dev.kehlchen.lib.api.integration.redis.RedisConfig;
import dev.kehlchen.lib.api.integration.redis.RedisMessageHandler;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.Delay;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

final class LettuceRedisTransport implements RedisTransport {
    private static final System.Logger LOGGER = System.getLogger(LettuceRedisTransport.class.getName());
    private final RedisConfig config;
    private final ClientResources resources;
    private final RedisClient client;
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<RedisSubscription>> subscriptions =
            new ConcurrentHashMap<>();
    private volatile StatefulRedisConnection<String, String> commands;
    private volatile StatefulRedisPubSubConnection<String, String> pubSub;
    private volatile CompletableFuture<Void> connecting;
    private volatile boolean closed;

    LettuceRedisTransport(RedisConfig config) {
        this.config = config;
        resources = ClientResources.builder().reconnectDelay(Delay.constant(config.reconnectDelay())).build();
        client = RedisClient.create(resources, createRedisUri(config));
        client.setOptions(ClientOptions.builder()
                .autoReconnect(true)
                .socketOptions(SocketOptions.builder().connectTimeout(config.connectTimeout()).build())
                .timeoutOptions(TimeoutOptions.enabled(config.commandTimeout()))
                .build());
    }

    @Override public synchronized CompletableFuture<Void> connect() {
        if (closed) return CompletableFuture.failedFuture(new IllegalStateException("Redis transport is closed"));
        if (isConnected()) return CompletableFuture.completedFuture(null);
        if (connecting != null) return connecting;
        CompletableFuture<StatefulRedisConnection<String, String>> commandConnection =
                client.connectAsync(StringCodec.UTF8, createRedisUri(config)).toCompletableFuture();
        CompletableFuture<StatefulRedisPubSubConnection<String, String>> pubSubConnection =
                client.connectPubSubAsync(StringCodec.UTF8, createRedisUri(config)).toCompletableFuture();
        commandConnection.thenAccept(value -> commands = value);
        pubSubConnection.thenAccept(value -> pubSub = value);
        connecting = commandConnection.thenCombine(pubSubConnection, (command, pub) -> {
            attachListener(pub);
            subscriptions.keySet().forEach(channel -> pub.async().subscribe(channel));
            return (Void) null;
        }).whenComplete((ignored, failure) -> {
            synchronized (this) {
                connecting = null;
                if (failure != null) closeConnections();
            }
        });
        return connecting;
    }

    @Override public boolean isConnected() {
        StatefulRedisConnection<String, String> command = commands;
        StatefulRedisPubSubConnection<String, String> pub = pubSub;
        return !closed && command != null && command.isOpen() && pub != null && pub.isOpen();
    }

    @Override public CompletableFuture<Long> publish(String channel, String payload) {
        RedisAsyncCommands<String, String> connection = commandCommands();
        if (connection == null) return CompletableFuture.failedFuture(new IllegalStateException("Redis is disconnected"));
        return connection.publish(channel, payload).toCompletableFuture();
    }

    @Override public CompletableFuture<Resource> subscribe(String channel, RedisMessageHandler handler) {
        StatefulRedisPubSubConnection<String, String> connection = pubSub;
        if (!isConnected() || connection == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Redis Pub/Sub is disconnected"));
        }
        RedisSubscription subscription = new RedisSubscription(channel, handler);
        subscriptions.computeIfAbsent(channel, ignored -> new CopyOnWriteArrayList<>()).add(subscription);
        return connection.async().subscribe(channel).toCompletableFuture()
                .thenApply(ignored -> (Resource) subscription)
                .whenComplete((value, failure) -> {
                    if (failure != null) subscription.close();
                });
    }

    private RedisAsyncCommands<String, String> commandCommands() {
        StatefulRedisConnection<String, String> connection = commands;
        return isConnected() && connection != null ? connection.async() : null;
    }

    private void attachListener(StatefulRedisPubSubConnection<String, String> connection) {
        connection.addListener(new RedisPubSubAdapter<>() {
            @Override public void message(String channel, String message) {
                if (message.getBytes(StandardCharsets.UTF_8).length > config.maximumMessageBytes()) {
                    LOGGER.log(System.Logger.Level.WARNING, "Dropped oversized Redis message on channel {0}", channel);
                    return;
                }
                CopyOnWriteArrayList<RedisSubscription> channelSubscriptions = subscriptions.get(channel);
                if (channelSubscriptions == null) return;
                for (RedisSubscription subscription : channelSubscriptions) {
                    if (subscription.closed) continue;
                    try {
                        subscription.handler.onMessage(channel, message);
                    } catch (RuntimeException failure) {
                        LOGGER.log(System.Logger.Level.WARNING,
                                "Redis subscription handler failed: {0}", failure.getClass().getName());
                    }
                }
            }
        });
    }

    private static RedisURI createRedisUri(RedisConfig config) {
        char[] password = config.password();
        RedisURI.Builder uri = RedisURI.Builder.redis(config.host(), config.port())
                .withDatabase(config.database())
                .withTimeout(config.commandTimeout());
        if (config.tls()) uri.withSsl(true);
        if (password != null) {
            if (config.username() == null || config.username().isBlank()) uri.withPassword(password);
            else uri.withAuthentication(config.username(), password);
        }
        RedisURI built = uri.build();
        if (password != null) java.util.Arrays.fill(password, '\0');
        return built;
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        subscriptions.values().forEach(listeners -> listeners.forEach(subscription -> subscription.closed = true));
        subscriptions.clear();
        RuntimeException failure = closeConnections();
        try {
            client.shutdown(0, 2, TimeUnit.SECONDS);
        } catch (RuntimeException shutdownFailure) {
            if (failure == null) failure = shutdownFailure;
            else failure.addSuppressed(shutdownFailure);
        }
        try {
            resources.shutdown(0, 2, TimeUnit.SECONDS);
        } catch (RuntimeException shutdownFailure) {
            if (failure == null) failure = shutdownFailure;
            else failure.addSuppressed(shutdownFailure);
        }
        if (failure != null) throw failure;
    }

    private RuntimeException closeConnections() {
        RuntimeException failure = null;
        StatefulRedisPubSubConnection<String, String> pub = pubSub;
        pubSub = null;
        if (pub != null) {
            try { pub.close(); } catch (RuntimeException error) { failure = error; }
        }
        StatefulRedisConnection<String, String> command = commands;
        commands = null;
        if (command != null) {
            try {
                command.close();
            } catch (RuntimeException error) {
                if (failure == null) failure = error;
                else failure.addSuppressed(error);
            }
        }
        return failure;
    }

    private final class RedisSubscription implements Resource {
        private final String channel;
        private final RedisMessageHandler handler;
        private volatile boolean closed;

        private RedisSubscription(String channel, RedisMessageHandler handler) {
            this.channel = channel;
            this.handler = handler;
        }

        @Override public void close() {
            if (closed) return;
            closed = true;
            CopyOnWriteArrayList<RedisSubscription> listeners = subscriptions.get(channel);
            if (listeners == null) return;
            listeners.remove(this);
            if (listeners.isEmpty() && subscriptions.remove(channel, listeners)) {
                StatefulRedisPubSubConnection<String, String> connection = pubSub;
                if (connection != null && connection.isOpen()) connection.async().unsubscribe(channel);
            }
        }
    }
}

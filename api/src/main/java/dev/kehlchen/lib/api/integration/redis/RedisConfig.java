package dev.kehlchen.lib.api.integration.redis;

import java.time.Duration;

/** Redis connection settings. Password data is never included in the config's string representation. */
public final class RedisConfig {
    private final String host;
    private final int port;
    private final String username;
    private final char[] password;
    private final int database;
    private final boolean tls;
    private final Duration connectTimeout;
    private final Duration commandTimeout;
    private final Duration reconnectDelay;
    private final int maximumReconnectAttempts;
    private final int maximumMessageBytes;

    public RedisConfig(String host, int port, String username, char[] password, int database, boolean tls,
                       Duration connectTimeout, Duration commandTimeout, Duration reconnectDelay,
                       int maximumReconnectAttempts, int maximumMessageBytes) {
        if (host == null || host.isBlank()) throw new IllegalArgumentException("Redis host is required");
        if (port < 1 || port > 65_535 || database < 0) throw new IllegalArgumentException("Invalid Redis port or database");
        if (connectTimeout == null || connectTimeout.compareTo(Duration.ofMillis(1)) < 0
                || commandTimeout == null || commandTimeout.compareTo(Duration.ofMillis(1)) < 0
                || reconnectDelay == null || reconnectDelay.compareTo(Duration.ofMillis(1)) < 0
                || connectTimeout.compareTo(Duration.ofMinutes(5)) > 0
                || commandTimeout.compareTo(Duration.ofMinutes(5)) > 0
                || reconnectDelay.compareTo(Duration.ofMinutes(5)) > 0
                || maximumReconnectAttempts < 1 || maximumReconnectAttempts > 10
                || maximumMessageBytes < 1 || maximumMessageBytes > 16 * 1024 * 1024) {
            throw new IllegalArgumentException("Invalid Redis timeout or message limit");
        }
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password == null ? null : password.clone();
        this.database = database;
        this.tls = tls;
        this.connectTimeout = connectTimeout;
        this.commandTimeout = commandTimeout;
        this.reconnectDelay = reconnectDelay;
        this.maximumReconnectAttempts = maximumReconnectAttempts;
        this.maximumMessageBytes = maximumMessageBytes;
    }

    public String host() { return host; }
    public int port() { return port; }
    public String username() { return username; }
    public char[] password() { return password == null ? null : password.clone(); }
    public int database() { return database; }
    public boolean tls() { return tls; }
    public Duration connectTimeout() { return connectTimeout; }
    public Duration commandTimeout() { return commandTimeout; }
    public Duration reconnectDelay() { return reconnectDelay; }
    public int maximumReconnectAttempts() { return maximumReconnectAttempts; }
    public int maximumMessageBytes() { return maximumMessageBytes; }
}

package dev.kehlchen.lib.core.serialization;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SerializationRegistry {
    private final Map<Class<?>, Serializer<?>> serializers = new ConcurrentHashMap<>();
    public <T> SerializationRegistry register(Class<T> type, Serializer<T> serializer) {
        serializers.put(type, serializer);
        return this;
    }
    @SuppressWarnings("unchecked")
    public <T> Serializer<T> get(Class<T> type) { return (Serializer<T>) serializers.get(type); }
    public <T> boolean has(Class<T> type) { return serializers.containsKey(type); }
    public void unregister(Class<?> type) { serializers.remove(type); }
}

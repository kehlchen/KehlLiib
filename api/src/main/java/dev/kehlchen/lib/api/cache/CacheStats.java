package dev.kehlchen.lib.api.cache;

public record CacheStats(long hits, long misses, long evictions, int size) {
    public double hitRate() {
        long requests = hits + misses;
        return requests == 0 ? 0.0 : (double) hits / requests;
    }
}

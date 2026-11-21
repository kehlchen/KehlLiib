package dev.kehlchen.lib.api.repository;

public record PageRequest(int page, int size, SortOrder order) {
    public PageRequest {
        if (page < 0 || size < 1) throw new IllegalArgumentException("Invalid page request");
        if (order == null) order = SortOrder.ASCENDING;
    }

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size, SortOrder.ASCENDING);
    }

    public static PageRequest of(int page, int size, SortOrder order) {
        return new PageRequest(page, size, order);
    }
}

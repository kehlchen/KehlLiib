package dev.kehlchen.lib.api.repository;

import java.util.List;

public record Page<T>(List<T> content, int page, int size, long totalElements) {
    public Page {
        content = List.copyOf(content);
    }
    public int totalPages() {
        return totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }
}

package dev.kehlchen.lib.api.nms;

import dev.kehlchen.lib.api.Lifecycle;

public interface ServerAdapter extends Lifecycle {
    String id();
    boolean supports(String minecraftVersion);
}

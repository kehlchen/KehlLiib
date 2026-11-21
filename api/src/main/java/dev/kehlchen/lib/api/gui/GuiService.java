package dev.kehlchen.lib.api.gui;

import dev.kehlchen.lib.api.Lifecycle;

/** Service for creating and registering GUI templates. */
public interface GuiService extends Lifecycle {
    Gui create(GuiDefinition definition);
    void unregister(Gui gui);
}

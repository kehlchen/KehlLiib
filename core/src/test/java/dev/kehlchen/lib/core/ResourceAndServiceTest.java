package dev.kehlchen.lib.core;

import dev.kehlchen.lib.api.Resource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceAndServiceTest {
    @Test
    void resourcesCloseInReverseOrder() {
        List<Integer> closed = new ArrayList<>();
        ResourceManagerImpl resources = new ResourceManagerImpl();
        resources.register((Resource) () -> closed.add(1));
        resources.register((Resource) () -> closed.add(2));

        resources.close();

        assertEquals(List.of(2, 1), closed);
    }

    @Test
    void serviceRegistryRequiresRegisteredType() {
        ServiceRegistryImpl registry = new ServiceRegistryImpl();

        assertThrows(IllegalStateException.class, () -> registry.getRequired(String.class));
        registry.register(String.class, "value");

        assertEquals("value", registry.getRequired(String.class));
    }
}

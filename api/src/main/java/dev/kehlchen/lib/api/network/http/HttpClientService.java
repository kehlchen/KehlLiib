package dev.kehlchen.lib.api.network.http;

import dev.kehlchen.lib.api.Lifecycle;

public interface HttpClientService extends Lifecycle {
    HttpCall execute(HttpRequestData request);
}

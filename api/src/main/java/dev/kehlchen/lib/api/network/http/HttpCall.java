package dev.kehlchen.lib.api.network.http;

import dev.kehlchen.lib.api.Result;

import java.util.concurrent.CompletableFuture;

/** A cancellable asynchronous HTTP operation. */
public final class HttpCall extends CompletableFuture<Result<HttpResponseData>> {
    private final Runnable cancellation;

    public HttpCall() { this(() -> {}); }

    public HttpCall(Runnable cancellation) {
        this.cancellation = java.util.Objects.requireNonNull(cancellation, "cancellation");
    }

    @Override public boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancelled = super.cancel(mayInterruptIfRunning);
        if (cancelled) cancellation.run();
        return cancelled;
    }
}

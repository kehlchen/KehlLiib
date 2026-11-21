package dev.kehlchen.lib.core.network;

import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.network.http.HttpCall;
import dev.kehlchen.lib.api.network.http.HttpClientConfig;
import dev.kehlchen.lib.api.network.http.HttpClientService;
import dev.kehlchen.lib.api.network.http.HttpRequestData;
import dev.kehlchen.lib.api.network.http.HttpResponseData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** JDK HTTP client with bounded concurrency, response size, retries and managed shutdown. */
public final class AsyncHttpClient implements HttpClientService {
    private final HttpClientConfig config;
    private final ExecutorService executor;
    private final ScheduledExecutorService timer;
    private final HttpClient client;
    private final Semaphore permits;
    private final Semaphore pendingSlots;
    private final AtomicLong nextRequestNanos = new AtomicLong();
    private final Set<HttpCall> calls = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    public AsyncHttpClient(HttpClientConfig config) {
        this.config = config;
        this.executor = Executors.newFixedThreadPool(config.workerThreads(), runnable -> {
            Thread thread = new Thread(runnable, "kehllib-http");
            thread.setDaemon(true);
            return thread;
        });
        this.timer = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "kehllib-http-timer");
            thread.setDaemon(true);
            return thread;
        });
        this.client = HttpClient.newBuilder().executor(executor)
                .connectTimeout(config.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.permits = new Semaphore(config.maximumConcurrentRequests());
        this.pendingSlots = new Semaphore(config.maximumConcurrentRequests() + config.maximumQueuedRequests());
    }

    @Override public String name() { return "http-client"; }
    @Override public String version() { return "1.0"; }

    @Override public HttpCall execute(HttpRequestData request) {
        AtomicReference<CompletableFuture<?>> active = new AtomicReference<>();
        AtomicReference<ScheduledFuture<?>> scheduled = new AtomicReference<>();
        HttpCall call = new HttpCall(() -> {
            CompletableFuture<?> future = active.get();
            if (future != null) future.cancel(true);
            ScheduledFuture<?> pending = scheduled.get();
            if (pending != null) pending.cancel(true);
        });
        if (closed) {
            call.complete(Result.failure(new IllegalStateException("HTTP client is closed")));
            return call;
        }
        if (request.body().length > config.maximumRequestBytes()) {
            call.complete(Result.failure(new IllegalArgumentException("HTTP request exceeds configured size limit")));
            return call;
        }
        if (!pendingSlots.tryAcquire()) {
            call.complete(Result.failure(new IllegalStateException("HTTP request queue is full")));
            return call;
        }
        calls.add(call);
        call.whenComplete((result, failure) -> {
            calls.remove(call);
            pendingSlots.release();
        });
        try {
            validateDestination(request.uri());
            scheduleAttempt(call, request, 1, active, scheduled, 0);
        } catch (RuntimeException failure) {
            call.complete(Result.failure(failure));
        }
        return call;
    }

    private void scheduleAttempt(HttpCall call, HttpRequestData request, int attempt,
                                 AtomicReference<CompletableFuture<?>> active,
                                 AtomicReference<ScheduledFuture<?>> scheduled, long delayMillis) {
        if (call.isDone() || closed) return;
        long interval = Math.max(1, TimeUnit.SECONDS.toNanos(1) / config.requestsPerSecond());
        long now = System.nanoTime();
        long slot;
        while (true) {
            long previous = nextRequestNanos.get();
            slot = Math.max(previous, now);
            if (nextRequestNanos.compareAndSet(previous, slot + interval)) break;
        }
        long rateDelay = Math.max(0, slot - now);
        long waitMillis = Math.max(delayMillis,
                rateDelay == 0 ? 0 : TimeUnit.NANOSECONDS.toMillis(rateDelay) + 1);
        ScheduledFuture<?> pending = timer.schedule(
                () -> startAttempt(call, request, attempt, active, scheduled), waitMillis, TimeUnit.MILLISECONDS);
        scheduled.set(pending);
    }

    private void startAttempt(HttpCall call, HttpRequestData request, int attempt,
                              AtomicReference<CompletableFuture<?>> active,
                              AtomicReference<ScheduledFuture<?>> scheduled) {
        if (call.isDone() || closed) return;
        if (!permits.tryAcquire()) {
            scheduled.set(timer.schedule(
                    () -> startAttempt(call, request, attempt, active, scheduled), 10, TimeUnit.MILLISECONDS));
            return;
        }
        try {
            CompletableFuture<Void> validation = CompletableFuture.runAsync(
                    () -> validateResolvedDestination(request.uri()), executor)
                    .orTimeout(Math.max(1, Math.min(config.connectTimeout().toMillis(),
                            request.timeout().toMillis())), TimeUnit.MILLISECONDS);
            active.set(validation);
            if (call.isDone()) validation.cancel(true);
            validation.thenCompose(ignored -> {
                CompletableFuture<HttpResponse<byte[]>> network = client.sendAsync(toRequest(request),
                        info -> new BoundedBodySubscriber(config.maximumResponseBytes()));
                active.set(network);
                if (call.isDone()) network.cancel(true);
                return network;
            }).whenComplete((response, failure) -> {
                permits.release();
                if (call.isDone()) return;
                if (failure != null) {
                    Throwable cause = unwrap(failure);
                    if (attempt < request.retryPolicy().maxAttempts()) {
                        scheduleRetry(call, request, attempt, active, scheduled);
                    } else {
                        call.complete(Result.failure(cause));
                    }
                    return;
                }
                HttpResponseData result = new HttpResponseData(response.statusCode(),
                        response.headers().map(), response.body());
                if (request.retryPolicy().retryStatusCodes().contains(result.statusCode())
                        && attempt < request.retryPolicy().maxAttempts()) {
                    scheduleRetry(call, request, attempt, active, scheduled);
                } else {
                    call.complete(Result.success(result));
                }
            });
        } catch (RuntimeException failure) {
            permits.release();
            call.complete(Result.failure(failure));
        }
    }

    private void scheduleRetry(HttpCall call, HttpRequestData request, int attempt,
                               AtomicReference<CompletableFuture<?>> active,
                               AtomicReference<ScheduledFuture<?>> scheduled) {
        var policy = request.retryPolicy();
        long multiplier = 1L << Math.min(attempt - 1, 20);
        long delay;
        try {
            delay = Math.min(policy.maximumDelay().toMillis(),
                    Math.multiplyExact(policy.initialDelay().toMillis(), multiplier));
        } catch (ArithmeticException overflow) {
            delay = policy.maximumDelay().toMillis();
        }
        if (policy.initialDelay().isZero()) delay = 0;
        scheduleAttempt(call, request, attempt + 1, active, scheduled, delay);
    }

    private HttpRequest toRequest(HttpRequestData request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri()).timeout(request.timeout());
        request.headers().forEach(builder::header);
        byte[] body = request.body();
        HttpRequest.BodyPublisher publisher = body.length == 0
                ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(body);
        builder.method(request.method().name(), publisher);
        return builder.build();
    }

    private void validateDestination(URI uri) {
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        boolean hostAllowed = config.allowedHosts().contains(host);
        if (!scheme.equals("https") && !(config.allowPlainHttp() && scheme.equals("http") && hostAllowed)) {
            throw new IllegalArgumentException("HTTPS is required; plain HTTP requires an explicitly allow-listed host");
        }
        if (!config.allowedHosts().isEmpty() && !config.allowedHosts().contains(host)) {
            throw new IllegalArgumentException("HTTP destination host is not allow-listed");
        }
        boolean explicitlyAllowedLocal = hostAllowed;
        if (!explicitlyAllowedLocal && (host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".local"))) {
            throw new IllegalArgumentException("Local HTTP destinations are not allowed");
        }
    }

    private void validateResolvedDestination(URI uri) {
        String host = uri.getHost();
        if (host.startsWith("[") && host.endsWith("]")) host = host.substring(1, host.length() - 1);
        boolean explicitlyAllowedLocal = config.allowedHosts().contains(uri.getHost().toLowerCase(Locale.ROOT));
        if (explicitlyAllowedLocal) return;
        try {
            for (java.net.InetAddress address : java.net.InetAddress.getAllByName(host)) {
                if (NetworkAddressPolicy.isNonPublic(address)) {
                    throw new IllegalArgumentException("Private or local HTTP destinations are not allowed");
                }
            }
        } catch (IOException failure) {
            throw new IllegalArgumentException("Unable to validate HTTP destination", failure);
        }
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof CompletionException || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) current = current.getCause();
        return current;
    }

    @Override public void onEnable() {
        if (closed) throw new IllegalStateException("HTTP client is closed");
    }

    @Override public void onDisable() {
        if (closed) return;
        closed = true;
        for (HttpCall call : new ArrayList<>(calls)) {
            call.cancel(true);
            call.complete(Result.failure(new IllegalStateException("HTTP client shut down")));
        }
        timer.shutdownNow();
        executor.shutdownNow();
    }

    private static final class BoundedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
        private final int maximumBytes;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> body = new CompletableFuture<>();
        private Flow.Subscription subscription;
        private int received;

        private BoundedBodySubscriber(int maximumBytes) { this.maximumBytes = maximumBytes; }
        @Override public CompletableFuture<byte[]> getBody() { return body; }

        @Override public void onSubscribe(Flow.Subscription value) {
            subscription = value;
            value.request(1);
        }

        @Override public void onNext(List<ByteBuffer> buffers) {
            for (ByteBuffer buffer : buffers) {
                int count = buffer.remaining();
                if (count > maximumBytes - received) {
                    subscription.cancel();
                    body.completeExceptionally(new IOException("HTTP response exceeded configured size limit"));
                    return;
                }
                byte[] bytes = new byte[count];
                buffer.get(bytes);
                output.write(bytes, 0, bytes.length);
                received += count;
            }
            subscription.request(1);
        }

        @Override public void onError(Throwable failure) { body.completeExceptionally(failure); }
        @Override public void onComplete() { body.complete(output.toByteArray()); }
    }
}

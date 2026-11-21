package dev.kehlchen.lib.api.database;

@FunctionalInterface
public interface TransactionWork<T> {
    T execute(Transaction transaction) throws Exception;
}

package dev.kehlchen.lib.core.storage;

import dev.kehlchen.lib.api.database.Database;
import dev.kehlchen.lib.api.storage.StorageProvider;
import dev.kehlchen.lib.api.storage.StorageSerializer;

import java.nio.file.Path;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Built-in storage providers. Custom providers can implement {@link StorageProvider}. */
public final class StorageProviders {
    private StorageProviders() {}

    public static <K, V> StorageProvider<K, V> json(Path directory,
                                                    StorageSerializer<K> keys,
                                                    StorageSerializer<V> values) {
        return new FileProvider<>(directory, ".json", keys, values, FileFormat.JSON);
    }

    public static <K, V> StorageProvider<K, V> yaml(Path directory,
                                                    StorageSerializer<K> keys,
                                                    StorageSerializer<V> values) {
        return new FileProvider<>(directory, ".yml", keys, values, FileFormat.YAML);
    }

    public static <K, V> StorageProvider<K, V> properties(Path directory,
                                                           StorageSerializer<K> keys,
                                                           StorageSerializer<V> values) {
        return new FileProvider<>(directory, ".properties", keys, values, FileFormat.PROPERTIES);
    }

    public static <K, V> StorageProvider<K, V> memory() {
        return new MemoryProvider<>();
    }

    public static <K, V> StorageProvider<K, V> sqlite(Database database, String namespace,
                                                       StorageSerializer<K> keys,
                                                       StorageSerializer<V> values) {
        return new SqliteProvider<>(database, namespace, keys, values);
    }

    private enum FileFormat { JSON, YAML, PROPERTIES }

    private static final class FileProvider<K, V> implements StorageProvider<K, V> {
        private final Path directory;
        private final String extension;
        private final StorageSerializer<K> keySerializer;
        private final StorageSerializer<V> valueSerializer;
        private final FileFormat format;

        private FileProvider(Path directory, String extension, StorageSerializer<K> keySerializer,
                             StorageSerializer<V> valueSerializer, FileFormat format) {
            this.directory = java.util.Objects.requireNonNull(directory, "directory").toAbsolutePath();
            this.extension = extension;
            this.keySerializer = java.util.Objects.requireNonNull(keySerializer, "keySerializer");
            this.valueSerializer = java.util.Objects.requireNonNull(valueSerializer, "valueSerializer");
            this.format = format;
        }

        @Override public synchronized void save(K key, V value) {
            Path file = file(key);
            try {
                java.nio.file.Files.createDirectories(directory);
                String encoded = valueSerializer.serialize(value);
                if (format == FileFormat.PROPERTIES) {
                    Properties properties = new Properties();
                    properties.setProperty("value", encoded);
                    try (var writer = new java.io.StringWriter()) {
                        properties.store(writer, null);
                        writeAtomically(file, writer.toString());
                    }
                } else {
                    writeAtomically(file, encoded);
                }
            } catch (Exception failure) {
                throw new IllegalStateException("Could not save storage value to " + file, failure);
            }
        }

        @Override public synchronized Optional<V> load(K key) {
            Path file = file(key);
            if (!java.nio.file.Files.exists(file)) return Optional.empty();
            try {
                String encoded;
                if (format == FileFormat.PROPERTIES) {
                    Properties properties = new Properties();
                    try (var reader = java.nio.file.Files.newBufferedReader(file)) {
                        properties.load(reader);
                    }
                    encoded = properties.getProperty("value");
                    if (encoded == null) throw new IllegalStateException("Missing value property");
                } else {
                    encoded = java.nio.file.Files.readString(file);
                }
                return Optional.ofNullable(valueSerializer.deserialize(encoded));
            } catch (Exception failure) {
                throw new IllegalStateException("Could not load storage value from " + file, failure);
            }
        }

        @Override public synchronized boolean delete(K key) {
            try { return java.nio.file.Files.deleteIfExists(file(key)); }
            catch (Exception failure) { throw new IllegalStateException("Could not delete storage value", failure); }
        }

        @Override public synchronized boolean exists(K key) {
            return java.nio.file.Files.isRegularFile(file(key));
        }

        @Override public synchronized Set<K> keys() {
            if (!java.nio.file.Files.isDirectory(directory)) return Set.of();
            Set<K> result = new LinkedHashSet<>();
            try (var files = java.nio.file.Files.list(directory)) {
                files.filter(java.nio.file.Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(extension))
                        .forEach(path -> {
                            String filename = path.getFileName().toString();
                            String encoded = filename.substring(0, filename.length() - extension.length());
                            try {
                                result.add(keySerializer.deserialize(new String(
                                        Base64.getUrlDecoder().decode(encoded),
                                        java.nio.charset.StandardCharsets.UTF_8)));
                            } catch (RuntimeException failure) {
                                throw new IllegalStateException("Invalid storage key filename: " + filename, failure);
                            }
                        });
            } catch (java.io.IOException failure) {
                throw new IllegalStateException("Could not list storage keys in " + directory, failure);
            }
            return Set.copyOf(result);
        }

        private Path file(K key) {
            String encodedKey = keySerializer.serialize(key);
            String filename = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    encodedKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return directory.resolve(filename + extension);
        }

        private static void writeAtomically(Path file, String value) throws java.io.IOException {
            Path temporary = java.nio.file.Files.createTempFile(file.getParent(), "kehllib-", ".tmp");
            try {
                java.nio.file.Files.writeString(temporary, value);
                try {
                    java.nio.file.Files.move(temporary, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                            java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                    java.nio.file.Files.move(temporary, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                java.nio.file.Files.deleteIfExists(temporary);
            }
        }
    }

    private static final class MemoryProvider<K, V> implements StorageProvider<K, V> {
        private final Map<K, V> values = new ConcurrentHashMap<>();
        @Override public void save(K key, V value) { values.put(key, value); }
        @Override public Optional<V> load(K key) { return Optional.ofNullable(values.get(key)); }
        @Override public boolean delete(K key) { return values.remove(key) != null; }
        @Override public boolean exists(K key) { return values.containsKey(key); }
        @Override public Set<K> keys() { return Set.copyOf(values.keySet()); }
        @Override public void saveAll(Map<K, V> values) { this.values.putAll(values); }
        @Override public void deleteAll(Collection<K> keys) { keys.forEach(values::remove); }
        @Override public void close() { values.clear(); }
    }

    private static final class SqliteProvider<K, V> implements StorageProvider<K, V> {
        private final Database database;
        private final String namespace;
        private final StorageSerializer<K> keySerializer;
        private final StorageSerializer<V> valueSerializer;

        private SqliteProvider(Database database, String namespace, StorageSerializer<K> keys,
                               StorageSerializer<V> values) {
            this.database = java.util.Objects.requireNonNull(database, "database");
            if (namespace == null || namespace.isBlank()) throw new IllegalArgumentException("namespace");
            this.namespace = namespace;
            keySerializer = java.util.Objects.requireNonNull(keys, "keys");
            valueSerializer = java.util.Objects.requireNonNull(values, "values");
            database.update("CREATE TABLE IF NOT EXISTS kehllib_storage "
                    + "(namespace VARCHAR(255) NOT NULL, storage_key TEXT NOT NULL, "
                    + "storage_value TEXT NOT NULL, PRIMARY KEY(namespace, storage_key))");
        }

        @Override public void save(K key, V value) {
            String encodedKey = keySerializer.serialize(key);
            String encodedValue = valueSerializer.serialize(value);
            database.transaction(transaction -> {
                transaction.update("DELETE FROM kehllib_storage WHERE namespace=? AND storage_key=?",
                        namespace, encodedKey);
                transaction.update("INSERT INTO kehllib_storage "
                                + "(namespace, storage_key, storage_value) VALUES (?, ?, ?)",
                        namespace, encodedKey, encodedValue);
                return null;
            });
        }

        @Override public Optional<V> load(K key) {
            return database.queryOne("SELECT storage_value FROM kehllib_storage "
                            + "WHERE namespace=? AND storage_key=?",
                    result -> result.getString(1), namespace, keySerializer.serialize(key))
                    .map(valueSerializer::deserialize);
        }
        @Override public boolean delete(K key) {
            return database.update("DELETE FROM kehllib_storage WHERE namespace=? AND storage_key=?",
                    namespace, keySerializer.serialize(key)) > 0;
        }
        @Override public boolean exists(K key) {
            return database.queryOne("SELECT 1 FROM kehllib_storage WHERE namespace=? AND storage_key=?",
                    result -> result.getInt(1), namespace, keySerializer.serialize(key)).isPresent();
        }
        @Override public Set<K> keys() {
            return database.query("SELECT storage_key FROM kehllib_storage WHERE namespace=?",
                            result -> keySerializer.deserialize(result.getString(1)), namespace)
                    .stream().collect(java.util.stream.Collectors.toUnmodifiableSet());
        }
    }
}

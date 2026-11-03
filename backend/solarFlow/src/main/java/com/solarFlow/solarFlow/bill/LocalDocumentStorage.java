package com.solarFlow.solarFlow.bill;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class LocalDocumentStorage implements DocumentStorage {

    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-f0-9]{32}$");

    private final Path root;

    public LocalDocumentStorage(@Value("${app.storage.local-dir}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new StorageException("Cannot create storage directory " + root, e);
        }
    }

    @Override
    public String save(InputStream in) {
        String key = UUID.randomUUID().toString().replace("-", "");
        Path target = resolve(key);
        try {
            Files.copy(in, target);          // no REPLACE_EXISTING: never overwrites
        } catch (IOException e) {
            try { Files.deleteIfExists(target); } catch (IOException ignored) { }
            throw new StorageException("Failed to store file", e);
        }
        return key;
    }

    @Override
    public InputStream open(String key) {
        try {
            return Files.newInputStream(resolve(key));
        } catch (NoSuchFileException e) {
            throw new StorageException("File not found in storage");
        } catch (IOException e) {
            throw new StorageException("Failed to read file", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new StorageException("Failed to delete file", e);
        }
    }

    private Path resolve(String key) {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            throw new StorageException("Invalid storage key");
        }
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw new StorageException("Invalid storage key");
        }
        return p;
    }
}
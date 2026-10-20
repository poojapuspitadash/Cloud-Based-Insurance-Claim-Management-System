package com.claimsys.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.*;

/** Used in the default "local" profile: stores uploads on disk. */
@Service
@Profile("!aws")
public class LocalFileStorageService implements FileStorageService {
    private final Path root;

    public LocalFileStorageService(@Value("${app.storage.local-dir:./uploads}") String dir) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
    }

    @Override
    public void store(String key, byte[] content, String contentType) throws IOException {
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Files.write(target, content);
    }

    @Override
    public byte[] load(String key) throws IOException {
        return Files.readAllBytes(resolve(key));
    }

    private Path resolve(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) throw new IllegalArgumentException("Invalid storage key");
        return p;
    }
}

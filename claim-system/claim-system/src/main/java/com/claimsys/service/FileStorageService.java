package com.claimsys.service;

import java.io.IOException;

/** Abstraction so the app runs locally (disk) and on AWS (S3) with no code change. */
public interface FileStorageService {
    void store(String key, byte[] content, String contentType) throws IOException;
    byte[] load(String key) throws IOException;
}

package com.claimsys.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

/**
 * Used in the "aws" profile. Credentials come from the EC2 instance's IAM role
 * (DefaultCredentialsProvider) - no access keys are stored in the code.
 */
@Service
@Profile("aws")
public class S3FileStorageService implements FileStorageService {
    private final S3Client s3;
    private final String bucket;

    public S3FileStorageService(S3Client s3, @Value("${app.storage.s3-bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    @Override
    public void store(String key, byte[] content, String contentType) {
        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket).key(key).contentType(contentType)
                        .serverSideEncryption(ServerSideEncryption.AES256)
                        .build(),
                RequestBody.fromBytes(content));
    }

    @Override
    public byte[] load(String key) {
        return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray();
    }
}

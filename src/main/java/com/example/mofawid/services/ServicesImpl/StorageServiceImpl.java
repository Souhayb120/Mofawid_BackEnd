package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.services.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;

@Service
public class StorageServiceImpl implements StorageService {

    private final S3Client s3Client;
    private final String bucket;

    public StorageServiceImpl(
            @Value("${r2.account-id}") String accountId,
            @Value("${r2.access-key}") String accessKey,
            @Value("${r2.secret-key}") String secretKey,
            @Value("${r2.bucket}") String bucket
    ) {

        this.bucket = bucket;

        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(
                        accessKey,
                        secretKey
                );

        S3Configuration s3Configuration =
                S3Configuration.builder()
                        .chunkedEncodingEnabled(false)
                        .build();

        this.s3Client = S3Client.builder()
                .endpointOverride(
                        URI.create(
                                "https://" + accountId + ".r2.cloudflarestorage.com"
                        )
                )
                .region(Region.of("auto"))
                .credentialsProvider(
                        StaticCredentialsProvider.create(credentials)
                )
                .serviceConfiguration(s3Configuration)
                .build();
    }

    @Override
    public String upload(MultipartFile file, String storageKey) {

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromBytes(file.getBytes())
            );

            return storageKey;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @Override
    public byte[] download(String storageKey) {

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .build();

        return s3Client.getObjectAsBytes(request).asByteArray();
    }

    @Override
    public void delete(String storageKey) {

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .build();

        s3Client.deleteObject(request);
    }
}
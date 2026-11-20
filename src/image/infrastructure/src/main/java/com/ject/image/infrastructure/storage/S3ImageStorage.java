package com.ject.image.infrastructure.storage;

import com.ject.image.application.dto.StoredImage;
import com.ject.image.application.port.ImageStorage;
import com.ject.image.infrastructure.config.ImageStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class S3ImageStorage implements ImageStorage {

    private static final int NOT_FOUND = 404;

    private final S3Client s3Client;
    private final ImageStorageProperties properties;

    @Override
    public void put(String key, InputStream content, long size, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .contentType(contentType)
                .contentLength(size)
                .build();
        s3Client.putObject(request, RequestBody.fromInputStream(content, size));
    }

    @Override
    public Optional<StoredImage> get(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .build();
        try {
            ResponseInputStream<GetObjectResponse> content = s3Client.getObject(request);
            GetObjectResponse response = content.response();
            return Optional.of(new StoredImage(content, response.contentLength(), response.contentType()));
        } catch (S3Exception e) {
            if (e.statusCode() == NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public boolean exists(String key) {
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .build();
        try {
            s3Client.headObject(request);
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == NOT_FOUND) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public void copy(String fromKey, String toKey) {
        CopyObjectRequest request = CopyObjectRequest.builder()
                .sourceBucket(properties.bucket())
                .sourceKey(fromKey)
                .destinationBucket(properties.bucket())
                .destinationKey(toKey)
                .build();
        s3Client.copyObject(request);
    }

    @Override
    public void delete(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .build();
        s3Client.deleteObject(request);
    }
}

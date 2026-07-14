package com.jejulocaltime.api.common.storage;

import com.jejulocaltime.api.common.exception.BusinessException;
import com.jejulocaltime.api.common.exception.ErrorCode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

/**
 * 운영 기본 구현체. local 프로필이 아닐 때 사용되며, S3Client는 S3Config에서
 * DefaultCredentialsProvider(AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY 환경변수)로 생성된다.
 */
@Component
@Profile("!local")
public class S3FileStorage implements FileStorage {

    private final S3Client s3Client;
    private final String bucket;
    private final String region;

    public S3FileStorage(
            S3Client s3Client,
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.region}") String region) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.region = region;
    }

    @Override
    public String store(MultipartFile file, String directory) {
        String extension = extractExtension(file.getOriginalFilename());
        String key = directory + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException | S3Exception e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미지 저장에 실패했습니다.");
        }

        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}

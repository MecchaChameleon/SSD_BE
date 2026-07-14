package com.jejulocaltime.api.common.storage;

import com.jejulocaltime.api.common.exception.BusinessException;
import com.jejulocaltime.api.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "file-storage.type", havingValue = "s3")
public class S3FileStorage implements FileStorage {
    private final S3Client s3;
    private final String bucket;
    private final String publicBaseUrl;

    public S3FileStorage(@Value("${file-storage.s3.region}") String region,
                         @Value("${file-storage.s3.bucket}") String bucket,
                         @Value("${file-storage.s3.public-base-url:}") String publicBaseUrl) {
        this.s3 = S3Client.builder().region(Region.of(region)).build();
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl.isBlank()
                ? "https://" + bucket + ".s3." + region + ".amazonaws.com"
                : publicBaseUrl.replaceAll("/$", "");
    }

    @Override
    public String store(MultipartFile file, String directory) {
        String extension = extension(file.getOriginalFilename());
        String key = directory + "/" + UUID.randomUUID() + extension;
        try {
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key)
                            .contentType(file.getContentType()).contentLength(file.getSize()).build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return publicBaseUrl + "/" + key;
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미지 저장에 실패했습니다.");
        }
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.')).toLowerCase();
    }
}

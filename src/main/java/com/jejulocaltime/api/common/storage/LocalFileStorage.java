package com.jejulocaltime.api.common.storage;

import com.jejulocaltime.api.common.exception.BusinessException;
import com.jejulocaltime.api.common.exception.ErrorCode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 실제 스토리지(S3 등) 연동 전까지 사용하는 더미/로컬 구현체.
 * 서버가 재배포되면 로컬 디스크 내용이 사라지므로 운영 환경에는 적합하지 않다.
 * TODO: S3FileStorage(FileStorage 구현체)로 교체 예정.
 */
@Component
@ConditionalOnProperty(name = "file-storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private final Path rootDir;
    private final String publicBaseUrl;

    public LocalFileStorage(
            @Value("${file-storage.local.root-dir:./uploads}") String rootDir,
            @Value("${file-storage.local.public-base-url:/uploads}") String publicBaseUrl) {
        this.rootDir = Path.of(rootDir);
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public String store(MultipartFile file, String directory) {
        try {
            Path targetDir = rootDir.resolve(directory).normalize();
            Files.createDirectories(targetDir);

            String extension = extractExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + extension;
            Path targetFile = targetDir.resolve(fileName);

            file.transferTo(targetFile);

            return publicBaseUrl + "/" + directory + "/" + fileName;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미지 저장에 실패했습니다.");
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}

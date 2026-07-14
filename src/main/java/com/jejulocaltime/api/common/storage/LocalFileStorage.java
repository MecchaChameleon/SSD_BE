package com.jejulocaltime.api.common.storage;

import com.jejulocaltime.api.common.exception.BusinessException;
import com.jejulocaltime.api.common.exception.ErrorCode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 로컬 개발 전용 구현체. 서버가 재배포되면 로컬 디스크 내용이 사라지므로
 * local 프로필(spring.profiles.active=local)에서만 활성화되고, 그 외에는 S3FileStorage를 사용한다.
 */
@Component
@Profile("local")
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

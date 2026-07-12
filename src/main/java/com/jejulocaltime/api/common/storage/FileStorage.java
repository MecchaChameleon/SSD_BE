package com.jejulocaltime.api.common.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 저장소 추상화. 지금은 로컬 디스크 구현체(LocalFileStorage)만 있지만,
 * 나중에 S3 등 실제 스토리지 구현체로 교체할 수 있도록 인터페이스로 분리했다.
 */
public interface FileStorage {

    /**
     * 파일을 저장하고 접근 가능한 URL(또는 경로)을 반환한다.
     *
     * @param file      업로드된 파일
     * @param directory 저장 경로 하위 디렉터리 (예: "products/{productId}")
     * @return 저장된 파일에 접근할 수 있는 URL
     */
    String store(MultipartFile file, String directory);
}

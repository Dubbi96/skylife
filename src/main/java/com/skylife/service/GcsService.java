package com.skylife.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * GCS 업로드/다운로드/미리보기 서비스.
 * - 다운로드: 항상 바이너리(attachment) 용 바이트만 반환
 * - 미리보기: Content-Type을 포함해 inline 렌더링이 가능하도록 반환
 */
@Service
public class GcsService {

    private final Storage storage;

    public GcsService() {
        this.storage = StorageOptions.getDefaultInstance().getService(); // ADC 사용
    }

    /** 업로드 */
    public void uploadFile(String bucketName, String objectName, MultipartFile file) throws IOException {
        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();
        storage.create(blobInfo, file.getBytes());
    }

    /** 공통: 객체 조회하여 바이트/메타데이터 반환 */
    private ObjectData fetchObject(String bucketName, String objectName) {
        Blob blob = storage.get(BlobId.of(bucketName, objectName));
        if (blob == null || !blob.exists()) {
            throw new NoSuchElementException("GCS object not found: " + objectName);
        }
        byte[] bytes = blob.getContent();
        String contentType = blob.getContentType();   // 예: image/png, application/pdf 등
        String filename = blob.getName();             // 원본 객체명
        return new ObjectData(bytes, contentType, filename);
    }

    /** 다운로드 전용: 바이트만 반환(Controller에서 attachment 헤더 설정) */
    public Resource getForDownload(String bucketName, String objectName) {
        ObjectData data = fetchObject(bucketName, objectName);
        return new ByteArrayResource(data.bytes());
    }

    /** 미리보기 전용: 바이트 + Content-Type + 파일명 반환 */
    public PreviewData getForPreview(String bucketName, String objectName) {
        ObjectData data = fetchObject(bucketName, objectName);

        // 미리보기 허용 타입 판단(이미지/*, application/pdf 우선)
        String ct = (data.contentType() != null) ? data.contentType() : "application/octet-stream";
        boolean previewable = ct.startsWith("image/") || ct.equalsIgnoreCase("application/pdf");

        // 미리보기 불가 타입이어도 그대로 반환(Controller에서 ct 그대로 세팅)
        return new PreviewData(new ByteArrayResource(data.bytes()), ct, data.filename(), previewable);
    }

    /** 내부 DTO: 객체 바이트 + 메타 */
    private record ObjectData(byte[] bytes, String contentType, String filename) {}

    /** 미리보기 응답 DTO */
    public record PreviewData(Resource body, String contentType, String filename, boolean previewable) {}
}
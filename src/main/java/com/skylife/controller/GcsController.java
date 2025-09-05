package com.skylife.controller;

import com.skylife.service.GcsService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/gcs")
public class GcsController {

    private static final String BUCKET_NAME = "skylife-gcs-bucket";

    private final GcsService gcsService;

    public GcsController(GcsService gcsService) {
        this.gcsService = gcsService;
    }

    /**
     * 업로드: multipart 파일만 업로드
     */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String objectName = (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank())
                ? file.getOriginalFilename()
                : "upload-" + System.currentTimeMillis();
        gcsService.uploadFile(BUCKET_NAME, objectName, file);
        return ResponseEntity.ok("File uploaded successfully: " + objectName);
    }

    @GetMapping("/download/{objectName:.+}")
    public ResponseEntity<Resource> download(@PathVariable String objectName) {
        String decoded = URLDecoder.decode(objectName, StandardCharsets.UTF_8);
        Resource body = gcsService.getForDownload(BUCKET_NAME, decoded);
        String cd = "attachment; filename*=UTF-8''" + UriUtils.encode(decoded, StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, cd)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    /**
     * 미리보기: GCS에 저장된 실제 Content-Type 을 사용해 inline 으로 표시
     */
    @GetMapping("/preview/{objectName:.+}")
    public ResponseEntity<Resource> preview(@PathVariable String objectName) {
        String decoded = URLDecoder.decode(objectName, StandardCharsets.UTF_8);
        var preview = gcsService.getForPreview(BUCKET_NAME, decoded);
        String inlineCd = "inline; filename*=UTF-8''" + UriUtils.encode(preview.filename(), StandardCharsets.UTF_8);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            if (preview.contentType() != null && !preview.contentType().isBlank()) {
                mediaType = MediaType.parseMediaType(preview.contentType());
            }
        } catch (Exception ignore) {
            // 파싱 실패 시 기본값(octet-stream) 유지
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, inlineCd)
                .contentType(mediaType)
                .body(preview.body());
    }
}
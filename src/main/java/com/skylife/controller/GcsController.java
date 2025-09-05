package com.skylife.controller;

import com.skylife.service.GcsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * REST controller exposing endpoints for interacting with Google Cloud
 * Storage.  All endpoints are under the base path {@code /api/v1/gcs}.
 *
 * <p>The controller delegates the actual storage operations to
 * {@link com.skylife.service.GcsService}.  It relies on the
 * {@code gcs.bucket-name} property to determine the default bucket
 * into which files are uploaded and from which files are retrieved.</p>
 */
@RestController
@RequestMapping("/gcs")
public class GcsController {

    private final GcsService gcsService;

    /**
     * The default GCS bucket name.  This value should be configured in
     * {@code application.properties} or via environment variables.  It
     * can also be overridden on a per‑request basis by passing a
     * {@code bucketName} parameter.
     */
    @Value("${gcs.bucket-name}")
    private String bucketName;

    public GcsController(GcsService gcsService) {
        this.gcsService = gcsService;
    }

    /**
     * Upload a file to GCS.  The object name defaults to the original
     * filename.  Optionally, a custom bucket name can be supplied as
     * part of the query parameters.  On success, returns a simple
     * confirmation message.
     *
     * @param file   the file to upload
     * @param bucket optional override for the target bucket
     * @return HTTP 200 with a confirmation message
     * @throws IOException if reading the file fails
     */
    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "bucket", required = false) String bucket) throws IOException {
        String targetBucket = bucket != null ? bucket : bucketName;
        String objectName = file.getOriginalFilename();
        gcsService.uploadFile(targetBucket, objectName, file);
        return ResponseEntity.ok("File uploaded successfully: " + objectName);
    }

    /**
     * Download a file from GCS.  Returns the file contents with a
     * Content-Disposition header set to attachment so that browsers
     * trigger a download.
     *
     * @param objectName the name of the object in GCS
     * @param bucket     optional override for the source bucket
     * @return HTTP 200 with the file content
     */
    @GetMapping("/download/{objectName}")
    public ResponseEntity<Resource> download(@PathVariable String objectName,
                                             @RequestParam(value = "bucket", required = false) String bucket) {
        String sourceBucket = bucket != null ? bucket : bucketName;
        Resource resource = gcsService.downloadFile(sourceBucket, objectName);
        String contentDisposition = "attachment; filename=\"" + objectName + "\"";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }

    /**
     * Preview an image from GCS.  Responds with an inline image; the
     * client can display it directly without triggering a download.
     *
     * @param objectName the name of the image object
     * @param bucket     optional override for the source bucket
     * @return HTTP 200 with the image content and an inline
     *         Content‑Disposition header
     */
    @GetMapping("/preview/{objectName}")
    public ResponseEntity<Resource> preview(@PathVariable String objectName,
                                            @RequestParam(value = "bucket", required = false) String bucket) {
        String sourceBucket = bucket != null ? bucket : bucketName;
        Resource resource = gcsService.previewImage(sourceBucket, objectName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + objectName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
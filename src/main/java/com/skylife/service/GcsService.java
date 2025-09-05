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

/**
 * A service component encapsulating the interactions with Google Cloud
 * Storage (GCS).  It provides three high‑level operations: uploading
 * a file, downloading a file, and retrieving an image preview.  The
 * implementation uses the official Google Cloud client library and
 * relies on the default credentials set in the environment.
 */
@Service
public class GcsService {

    /**
     * A lazily initialized GCS Storage client.  The Storage class is
     * thread‑safe and can be reused across requests.  It will use
     * Application Default Credentials if available.
     */
    private final Storage storage;

    public GcsService() {
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    /**
     * Upload a file to the specified bucket.  The original filename
     * determines the object name unless overridden by the caller.  The
     * content type is propagated from the uploaded MultipartFile.
     *
     * @param bucketName the target GCS bucket
     * @param objectName the name of the object in GCS
     * @param file       the file to upload
     * @throws IOException if reading the file contents fails
     */
    public void uploadFile(String bucketName, String objectName, MultipartFile file) throws IOException {
        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();
        storage.create(blobInfo, file.getBytes());
    }

    /**
     * Download a file from GCS as a Spring {@link Resource}.  The caller
     * should supply the bucket and object name.  If the object does not
     * exist, a runtime exception will be thrown.
     *
     * @param bucketName the source bucket
     * @param objectName the object to fetch
     * @return a {@link Resource} containing the object's bytes
     */
    public Resource downloadFile(String bucketName, String objectName) {
        Blob blob = storage.get(BlobId.of(bucketName, objectName));
        if (blob == null) {
            throw new IllegalArgumentException("Object not found: " + objectName);
        }
        byte[] content = blob.getContent();
        return new ByteArrayResource(content);
    }

    /**
     * Retrieve an image for preview.  This method delegates to
     * {@link #downloadFile(String, String)} but is separated for
     * semantic clarity.  In a more sophisticated implementation one
     * might perform additional processing, e.g. generating a
     * thumbnail.
     *
     * @param bucketName the source bucket
     * @param objectName the image object to fetch
     * @return a {@link Resource} containing the image's bytes
     */
    public Resource previewImage(String bucketName, String objectName) {
        return downloadFile(bucketName, objectName);
    }
}
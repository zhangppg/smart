package com.example.photomanager.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PhotoItem {
    private Long recordId;
    private String id;
    private String ownerId;
    private String title;
    private String category;
    private List<String> tags = new ArrayList<>();
    private String originalFilename;
    private String contentType;
    private long size;
    private String storageFilename;
    private String fileUrl;
    // When we generate a browser-friendly preview (e.g. HEIC -> JPEG), we keep the
    // original upload as a separate "source" file for download.
    private String sourceContentType;
    private long sourceSize;
    private String sourceStorageFilename;
    private String sourceFileUrl;
    private Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getStorageFilename() {
        return storageFilename;
    }

    public void setStorageFilename(String storageFilename) {
        this.storageFilename = storageFilename;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getSourceContentType() {
        return sourceContentType;
    }

    public void setSourceContentType(String sourceContentType) {
        this.sourceContentType = sourceContentType;
    }

    public long getSourceSize() {
        return sourceSize;
    }

    public void setSourceSize(long sourceSize) {
        this.sourceSize = sourceSize;
    }

    public String getSourceStorageFilename() {
        return sourceStorageFilename;
    }

    public void setSourceStorageFilename(String sourceStorageFilename) {
        this.sourceStorageFilename = sourceStorageFilename;
    }

    public String getSourceFileUrl() {
        return sourceFileUrl;
    }

    public void setSourceFileUrl(String sourceFileUrl) {
        this.sourceFileUrl = sourceFileUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

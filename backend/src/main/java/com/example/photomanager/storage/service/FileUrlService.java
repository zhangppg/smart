package com.example.photomanager.storage.service;

import com.example.photomanager.model.PhotoItem;
import com.example.photomanager.model.PagedResponse;
import com.example.photomanager.storage.entity.FileUrlEntity;
import com.example.photomanager.storage.repository.FileUrlRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FileUrlService {
    private final FileUrlRepository fileUrlRepository;

    public FileUrlService(FileUrlRepository fileUrlRepository) {
        this.fileUrlRepository = fileUrlRepository;
    }

    public void saveOrUpdate(PhotoItem photoItem) {
        Long parsedUserId = parseUserId(photoItem.getOwnerId());
        FileUrlEntity entity = fileUrlRepository.findByPhotoId(photoItem.getId()).orElseGet(FileUrlEntity::new);
        entity.setPhotoId(photoItem.getId());
        entity.setUserId(parsedUserId);
        entity.setFileUrl(photoItem.getFileUrl());
        entity.setOriginalFilename(photoItem.getOriginalFilename());
        entity.setContentType(photoItem.getContentType());
        entity.setFileSize(photoItem.getSize());
        entity.setStorageFilename(photoItem.getStorageFilename());
        entity.setSourceFileUrl(photoItem.getSourceFileUrl());
        entity.setSourceContentType(photoItem.getSourceContentType());
        entity.setSourceFileSize(photoItem.getSourceSize() == 0L ? null : photoItem.getSourceSize());
        entity.setSourceStorageFilename(photoItem.getSourceStorageFilename());
        entity.setTitle(photoItem.getTitle());
        entity.setCategory(photoItem.getCategory());
        entity.setTags(joinTags(photoItem.getTags()));
        fileUrlRepository.save(entity);
    }

    public PhotoItem getPhotoById(String userId, String photoId) {
        Long parsedUserId = parseUserId(userId);
        FileUrlEntity entity = fileUrlRepository.findByPhotoIdAndUserId(photoId, parsedUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"));
        return toPhotoItem(entity);
    }

    public PagedResponse<PhotoItem> search(String userId,
                                           String q,
                                           String category,
                                           String tag,
                                           int page,
                                           int size) {
        Long parsedUserId = parseUserId(userId);
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Page<FileUrlEntity> result = fileUrlRepository.search(
                parsedUserId,
                normalize(q),
                normalize(category),
                normalize(tag),
                PageRequest.of(safePage - 1, safeSize)
        );

        List<PhotoItem> items = result.getContent().stream()
                .map(this::toPhotoItem)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                items,
                safePage,
                safeSize,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public void deleteByPhotoId(String photoId) {
        fileUrlRepository.deleteByPhotoId(photoId);
    }

    public void deleteByPhotoId(String userId, String photoId) {
        Long parsedUserId = parseUserId(userId);
        fileUrlRepository.deleteByPhotoIdAndUserId(photoId, parsedUserId);
    }

    public void deleteByRecordId(String userId, Long id) {
        Long parsedUserId = parseUserId(userId);
        FileUrlEntity entity = fileUrlRepository.findByIdAndUserId(id, parsedUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "file_url record not found"));
        fileUrlRepository.delete(entity);
    }

    private PhotoItem toPhotoItem(FileUrlEntity entity) {
        PhotoItem photoItem = new PhotoItem();
        photoItem.setRecordId(entity.getId());
        photoItem.setId(entity.getPhotoId());
        photoItem.setOwnerId(String.valueOf(entity.getUserId()));
        photoItem.setTitle(entity.getTitle());
        photoItem.setCategory(entity.getCategory());
        photoItem.setTags(splitTags(entity.getTags()));
        photoItem.setOriginalFilename(entity.getOriginalFilename());
        photoItem.setContentType(entity.getContentType());
        photoItem.setSize(entity.getFileSize() == null ? 0L : entity.getFileSize());
        photoItem.setStorageFilename(entity.getStorageFilename());
        photoItem.setFileUrl(entity.getFileUrl());
        photoItem.setSourceFileUrl(entity.getSourceFileUrl());
        photoItem.setSourceContentType(entity.getSourceContentType());
        photoItem.setSourceSize(entity.getSourceFileSize() == null ? 0L : entity.getSourceFileSize());
        photoItem.setSourceStorageFilename(entity.getSourceStorageFilename());
        photoItem.setCreatedAt(entity.getCreatedAt());
        return photoItem;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return tags.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.joining(","));
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user id format", ex);
        }
    }
}

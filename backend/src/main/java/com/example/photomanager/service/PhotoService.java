package com.example.photomanager.service;

import com.example.photomanager.model.PagedResponse;
import com.example.photomanager.model.PhotoItem;
import com.example.photomanager.storage.service.FileUrlService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PhotoService {
    private final Path uploadDir;
    private final Path metadataFile;
    private final ObjectMapper objectMapper;
    private final FileUrlService fileUrlService;
    private final Map<String, PhotoItem> photos = new ConcurrentHashMap<>();

    public PhotoService(@Value("${photo.storage.dir:data/uploads/photos}") String storageDir,
                        @Value("${photo.storage.metadata:data/photos.json}") String metadataPath,
                        ObjectMapper objectMapper,
                        FileUrlService fileUrlService) {
        this.uploadDir = Path.of(storageDir).toAbsolutePath().normalize();
        this.metadataFile = Path.of(metadataPath).toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
        this.fileUrlService = fileUrlService;
        initStorage();
        loadMetadata();
    }

    public synchronized PhotoItem savePhoto(String userId,
                                            MultipartFile file,
                                            String title,
                                            String category,
                                            String tagsRaw) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
        }

        String id = UUID.randomUUID().toString();
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String storageFilename = ext == null || ext.isBlank() ? id : id + "." + ext;
        Path target = uploadDir.resolve(storageFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store photo", e);
        }

        PhotoItem item = new PhotoItem();
        item.setId(id);
        item.setOwnerId(userId);
        item.setTitle((title == null || title.isBlank()) ? file.getOriginalFilename() : title.trim());
        item.setCategory(normalizeNullable(category));
        item.setTags(parseTags(tagsRaw));
        item.setOriginalFilename(file.getOriginalFilename());
        item.setContentType(contentType);
        item.setSize(file.getSize());
        item.setStorageFilename(storageFilename);
        item.setFileUrl(target.toString());
        item.setCreatedAt(Instant.now());

        photos.put(id, item);
        try {
            fileUrlService.saveOrUpdate(item);
            persistMetadata();
        } catch (RuntimeException ex) {
            photos.remove(id);
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
            }
            throw ex;
        }
        return item;
    }

    public synchronized PhotoItem updatePhoto(String userId,
                                              String id,
                                              String title,
                                              String category,
                                              String tagsRaw) {
        PhotoItem item = getPhotoOrThrow(userId, id);

        if (title != null) {
            String trimmedTitle = title.trim();
            item.setTitle(trimmedTitle.isEmpty() ? item.getOriginalFilename() : trimmedTitle);
        }
        if (category != null) {
            item.setCategory(normalizeNullable(category));
        }
        if (tagsRaw != null) {
            item.setTags(parseTags(tagsRaw));
        }

        photos.put(item.getId(), item);
        fileUrlService.saveOrUpdate(item);
        persistMetadata();
        return item;
    }

    public PagedResponse<PhotoItem> listPhotos(String userId,
                                               String q,
                                               String category,
                                               String tag,
                                               int page,
                                               int size) {
        return fileUrlService.search(userId, q, category, tag, page, size);
    }

    public Path getPhotoPath(String userId, String id) {
        PhotoItem photo = getPhotoOrThrow(userId, id);
        Path path = Path.of(photo.getFileUrl()).toAbsolutePath().normalize();
        if (!path.startsWith(uploadDir) || !Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo file not found");
        }
        return path;
    }

    public PhotoItem getPhotoOrThrow(String userId, String id) {
        return fileUrlService.getPhotoById(userId, id);
    }

    public synchronized void deletePhoto(String userId, String id) {
        PhotoItem photo = getPhotoOrThrow(userId, id);
        photos.remove(photo.getId());

        Path path = Path.of(photo.getFileUrl()).toAbsolutePath().normalize();
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete photo file", e);
        }

        fileUrlService.deleteByPhotoId(photo.getId());
        persistMetadata();
    }

    public void deleteFileUrlRecord(String userId, Long id) {
        fileUrlService.deleteByRecordId(userId, id);
    }

    private List<String> parseTags(String tagsRaw) {
        if (tagsRaw == null || tagsRaw.isBlank()) {
            return new ArrayList<>();
        }
        return List.of(tagsRaw.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void initStorage() {
        try {
            Files.createDirectories(uploadDir);
            Path metadataParent = metadataFile.getParent();
            if (metadataParent != null) {
                Files.createDirectories(metadataParent);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize storage", e);
        }
    }

    private void loadMetadata() {
        if (!Files.exists(metadataFile)) {
            return;
        }
        try {
            List<PhotoItem> saved = objectMapper.readValue(metadataFile.toFile(), new TypeReference<>() {});
            for (PhotoItem item : saved) {
                photos.put(item.getId(), item);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read metadata file", e);
        }
    }

    private void persistMetadata() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataFile.toFile(), new ArrayList<>(photos.values()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save metadata", e);
        }
    }
}

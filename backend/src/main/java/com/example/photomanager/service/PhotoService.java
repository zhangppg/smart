package com.example.photomanager.service;

import com.example.photomanager.model.PagedResponse;
import com.example.photomanager.model.PhotoItem;
import com.example.photomanager.storage.service.FileUrlService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PhotoService {
    private static final Logger log = LoggerFactory.getLogger(PhotoService.class);
    private static final DateTimeFormatter DAY_FOLDER = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd

    private final Path uploadDir;
    private final Path metadataFile;
    private final ObjectMapper objectMapper;
    private final FileUrlService fileUrlService;
    private final Map<String, PhotoItem> photos = new ConcurrentHashMap<>();
    private final Map<String, Object> previewLocks = new ConcurrentHashMap<>();

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
        String originalFilename = file.getOriginalFilename();
        String ext = safeLowerExt(StringUtils.getFilenameExtension(originalFilename));
        boolean isHeic = isHeicUpload(contentType, ext);

        Path dayDir = resolveDayDir();

        Path viewPath;
        String storageFilename;
        String viewContentType;
        long viewSize;

        Path sourcePath = null;
        String sourceStorageFilename = null;
        String sourceContentType = null;
        long sourceSize = 0L;

        if (isHeic) {
            // Store original HEIC for download...
            String sourceExt = (ext == null || ext.isBlank()) ? "heic" : ext;
            sourceStorageFilename = id + "." + sourceExt;
            sourcePath = dayDir.resolve(sourceStorageFilename);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, sourcePath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store photo", e);
            }

            sourceContentType = contentType;
            try {
                sourceSize = Files.size(sourcePath);
            } catch (IOException ignored) {
                sourceSize = file.getSize();
            }

            // ...and generate a JPEG preview for browser <img>.
            storageFilename = id + ".jpg";
            viewPath = dayDir.resolve(storageFilename);
            try {
                convertHeicToJpeg(sourcePath, viewPath);
                viewContentType = "image/jpeg";
                viewSize = Files.size(viewPath);
            } catch (Exception ex) {
                // If conversion fails, fall back to serving HEIC (may not display in most browsers).
                try {
                    Files.deleteIfExists(viewPath);
                } catch (IOException ignored) {
                }
                storageFilename = sourceStorageFilename;
                viewPath = sourcePath;
                viewContentType = contentType;
                viewSize = sourceSize;
                sourcePath = null;
                sourceStorageFilename = null;
                sourceContentType = null;
                sourceSize = 0L;
            }
        } else {
            storageFilename = (ext == null || ext.isBlank()) ? id : id + "." + ext;
            viewPath = dayDir.resolve(storageFilename);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, viewPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store photo", e);
            }
            viewContentType = contentType;
            viewSize = file.getSize();
        }

        PhotoItem item = new PhotoItem();
        item.setId(id);
        item.setOwnerId(userId);
        item.setTitle((title == null || title.isBlank()) ? originalFilename : title.trim());
        item.setCategory(normalizeNullable(category));
        item.setTags(parseTags(tagsRaw));
        item.setOriginalFilename(originalFilename);
        item.setContentType(viewContentType);
        item.setSize(viewSize);
        item.setStorageFilename(storageFilename);
        item.setFileUrl(viewPath.toString());
        item.setSourceFileUrl(sourcePath == null ? null : sourcePath.toString());
        item.setSourceStorageFilename(sourceStorageFilename);
        item.setSourceContentType(sourceContentType);
        item.setSourceSize(sourceSize);
        item.setCreatedAt(Instant.now());

        photos.put(id, item);
        try {
            fileUrlService.saveOrUpdate(item);
            persistMetadata();
        } catch (RuntimeException ex) {
            photos.remove(id);
            try {
                Files.deleteIfExists(viewPath);
                if (sourcePath != null) {
                    Files.deleteIfExists(sourcePath);
                }
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
        return updatePhoto(userId, id, title, category, tagsRaw, false);
    }

    public synchronized PhotoItem updatePhoto(String userId,
                                              String id,
                                              String title,
                                              String category,
                                              String tagsRaw,
                                              boolean allAccess) {
        PhotoItem item = getPhotoOrThrow(userId, id, allAccess);

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

    public PagedResponse<PhotoItem> listPhotosAll(String q,
                                                  String category,
                                                  String tag,
                                                  int page,
                                                  int size) {
        return fileUrlService.searchAll(q, category, tag, page, size);
    }

    public Path getPhotoPath(String userId, String id) {
        return getPhotoViewPath(userId, id);
    }

    public Path getPhotoViewPath(String userId, String id) {
        return getPhotoViewPath(userId, id, false);
    }

    public Path getPhotoViewPath(String userId, String id, boolean allAccess) {
        PhotoItem photo = getPhotoOrThrow(userId, id, allAccess);
        PhotoItem maybeConverted = ensureHeicPreviewIfNeeded(photo);
        return resolveAndValidate(maybeConverted.getFileUrl());
    }

    public Path getPhotoDownloadPath(String userId, String id) {
        return getPhotoDownloadPath(userId, id, false);
    }

    public Path getPhotoDownloadPath(String userId, String id, boolean allAccess) {
        PhotoItem photo = getPhotoOrThrow(userId, id, allAccess);
        String url = (photo.getSourceFileUrl() != null && !photo.getSourceFileUrl().isBlank())
                ? photo.getSourceFileUrl()
                : photo.getFileUrl();
        return resolveAndValidate(url);
    }

    public PhotoItem getPhotoOrThrow(String userId, String id) {
        return getPhotoOrThrow(userId, id, false);
    }

    public PhotoItem getPhotoOrThrow(String userId, String id, boolean allAccess) {
        if (allAccess) {
            return fileUrlService.getPhotoById(id);
        }
        return fileUrlService.getPhotoById(userId, id);
    }

    public synchronized void deletePhoto(String userId, String id) {
        deletePhoto(userId, id, false);
    }

    public synchronized void deletePhoto(String userId, String id, boolean allAccess) {
        // Keep delete simple and robust: delete DB row by photo id.
        // (File cleanup is optional and can be handled later.)
        try {
            fileUrlService.deleteByPhotoId(id);
        } catch (RuntimeException ex) {
            // Don't break the delete button due to DB glitches.
            log.warn("Failed to delete file_url row for photo {}: {}", id, ex.toString());
        }

        photos.remove(id);
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
            // Metadata file is legacy; don't break core API behavior if it fails.
            log.warn("Failed to save metadata file {}: {}", metadataFile, e.toString());
        }
    }

    private Path resolveAndValidate(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo file not found");
        }
        Path path = Path.of(fileUrl).toAbsolutePath().normalize();
        if (!path.startsWith(uploadDir) || !Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo file not found");
        }
        return path;
    }

    private Path resolveDayDir() {
        String day = LocalDate.now(ZoneId.systemDefault()).format(DAY_FOLDER);
        Path dayDir = uploadDir.resolve(day).toAbsolutePath().normalize();
        if (!dayDir.startsWith(uploadDir)) {
            throw new IllegalStateException("Invalid upload day directory");
        }
        try {
            Files.createDirectories(dayDir);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to initialize upload directory", e);
        }
        return dayDir;
    }

    private boolean isHeicUpload(String contentType, String ext) {
        String ct = (contentType == null) ? "" : contentType.toLowerCase();
        String e = (ext == null) ? "" : ext.toLowerCase();
        return ct.equals("image/heic") || ct.equals("image/heif") || ct.equals("image/heif-sequence")
                || e.equals("heic") || e.equals("heif");
    }

    private boolean isHeicPath(String fileUrl) {
        if (fileUrl == null) return false;
        String p = fileUrl.toLowerCase();
        return p.endsWith(".heic") || p.endsWith(".heif");
    }

    private String safeLowerExt(String ext) {
        if (ext == null) return null;
        String trimmed = ext.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }

    private void convertHeicToJpeg(Path inputHeic, Path outputJpeg) throws IOException, InterruptedException {
        // Prefer macOS built-in tool when available.
        // Example: sips -s format jpeg input.heic --out output.jpg
        ProcessBuilder pb = new ProcessBuilder(
                "sips",
                "-s", "format", "jpeg",
                inputHeic.toString(),
                "--out", outputJpeg.toString()
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream is = p.getInputStream()) {
            is.transferTo(out);
        }
        int code = p.waitFor();
        if (code != 0 || !Files.exists(outputJpeg) || Files.size(outputJpeg) == 0L) {
            String msg = out.toString();
            throw new IOException("HEIC conversion failed (sips exit " + code + "): " + msg);
        }
    }

    private PhotoItem ensureHeicPreviewIfNeeded(PhotoItem photo) {
        if (photo == null) return null;

        // Already has a separate source file (original) -> view is expected to be browser-friendly.
        if (photo.getSourceFileUrl() != null && !photo.getSourceFileUrl().isBlank()) {
            return photo;
        }

        boolean looksHeic = isHeicUpload(photo.getContentType(), safeLowerExt(StringUtils.getFilenameExtension(photo.getOriginalFilename())))
                || isHeicPath(photo.getFileUrl())
                || isHeicUpload(photo.getContentType(), safeLowerExt(StringUtils.getFilenameExtension(photo.getStorageFilename())));
        if (!looksHeic) return photo;

        Object lock = previewLocks.computeIfAbsent(photo.getId(), k -> new Object());
        synchronized (lock) {
            // Re-read latest record; another thread may have converted already.
            PhotoItem latest = fileUrlService.getPhotoById(photo.getOwnerId(), photo.getId());
            if (latest.getSourceFileUrl() != null && !latest.getSourceFileUrl().isBlank()) {
                return latest;
            }

            Path sourcePath = resolveAndValidate(latest.getFileUrl());
            Path parentDir = sourcePath.getParent() == null ? uploadDir : sourcePath.getParent();
            Path previewPath = parentDir.resolve(latest.getId() + ".jpg").toAbsolutePath().normalize();

            // If preview already exists from a previous attempt, just wire it up.
            if (Files.exists(previewPath)) {
                try {
                    if (Files.size(previewPath) > 0L) {
                        wirePreview(latest, sourcePath, previewPath);
                        fileUrlService.saveOrUpdate(latest);
                        return latest;
                    }
                } catch (IOException ignored) {
                }
            }

            try {
                convertHeicToJpeg(sourcePath, previewPath);
                wirePreview(latest, sourcePath, previewPath);
                fileUrlService.saveOrUpdate(latest);
                return latest;
            } catch (Exception ex) {
                // Don't block viewing if conversion fails; serve original (may not render in browser).
                try {
                    Files.deleteIfExists(previewPath);
                } catch (IOException ignored) {
                }
                return latest;
            }
        }
    }

    private void wirePreview(PhotoItem item, Path sourcePath, Path previewPath) throws IOException {
        item.setSourceFileUrl(sourcePath.toString());
        item.setSourceStorageFilename(item.getStorageFilename());
        item.setSourceContentType(item.getContentType() == null ? "image/heic" : item.getContentType());
        item.setSourceSize(Files.size(sourcePath));

        item.setFileUrl(previewPath.toString());
        item.setStorageFilename(item.getId() + ".jpg");
        item.setContentType("image/jpeg");
        item.setSize(Files.size(previewPath));
    }
}

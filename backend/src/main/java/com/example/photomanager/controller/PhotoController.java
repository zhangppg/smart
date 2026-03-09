package com.example.photomanager.controller;

import com.example.photomanager.auth.AuthContext;
import com.example.photomanager.model.PagedResponse;
import com.example.photomanager.model.PhotoItem;
import com.example.photomanager.model.PhotoUpdateRequest;
import com.example.photomanager.model.PhotoUploadResponse;
import com.example.photomanager.service.PhotoService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/photos")
public class PhotoController {
    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @PostMapping(value = {"", "/upload"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PhotoUploadResponse uploadPhoto(@RequestParam("file") MultipartFile file,
                                           @RequestParam(value = "title", required = false) String title,
                                           @RequestParam(value = "category", required = false) String category,
                                           @RequestParam(value = "tags", required = false) String tags) {
        PhotoItem saved = photoService.savePhoto(currentUserId(), file, title, category, tags);
        return new PhotoUploadResponse("Photo uploaded successfully", saved);
    }

    @PutMapping("/{id}")
    public PhotoItem updatePhoto(@PathVariable String id,
                                 @RequestBody PhotoUpdateRequest request) {
        return photoService.updatePhoto(currentUserId(), id, request.getTitle(), request.getCategory(), request.getTags());
    }

    @GetMapping
    public PagedResponse<PhotoItem> listPhotos(@RequestParam(value = "q", required = false) String q,
                                               @RequestParam(value = "category", required = false) String category,
                                               @RequestParam(value = "tag", required = false) String tag,
                                               @RequestParam(value = "page", defaultValue = "1") int page,
                                               @RequestParam(value = "size", defaultValue = "12") int size) {
        return photoService.listPhotos(currentUserId(), q, category, tag, page, size);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadPhoto(@PathVariable String id) {
        PhotoItem photo = photoService.getPhotoOrThrow(currentUserId(), id);
        Path path = photoService.getPhotoPath(currentUserId(), id);
        Resource resource = new FileSystemResource(path);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + photo.getOriginalFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<Resource> viewPhoto(@PathVariable String id) {
        PhotoItem photo = photoService.getPhotoOrThrow(currentUserId(), id);
        Path path = photoService.getPhotoPath(currentUserId(), id);
        Resource resource = new FileSystemResource(path);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePhoto(@PathVariable String id) {
        photoService.deletePhoto(currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/file-url/{id}")
    public ResponseEntity<Void> deleteFileUrl(@PathVariable Long id) {
        photoService.deleteFileUrlRecord(currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    private String currentUserId() {
        return AuthContext.getUserId();
    }
}

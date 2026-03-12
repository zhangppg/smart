package com.example.photomanager.storage.repository;

import com.example.photomanager.storage.entity.FileUrlEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FileUrlRepository extends JpaRepository<FileUrlEntity, Long> {
    Optional<FileUrlEntity> findByPhotoId(String photoId);

    Optional<FileUrlEntity> findByPhotoIdAndUserId(String photoId, Long userId);
    Optional<FileUrlEntity> findByIdAndUserId(Long id, Long userId);

    @Query(
            value = """
                    SELECT * FROM file_url f
                    WHERE f.user_id = :userId
                      AND (:q IS NULL OR LOWER(CONCAT_WS(' ', COALESCE(f.title,''), COALESCE(f.category,''), COALESCE(f.tags,''), COALESCE(f.original_filename,''))) LIKE LOWER(CONCAT('%', :q, '%')))
                      AND (:category IS NULL OR LOWER(f.category) = LOWER(:category))
                      AND (:tag IS NULL OR LOWER(f.tags) LIKE LOWER(CONCAT('%', :tag, '%')))
                    ORDER BY f.created_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(1) FROM file_url f
                    WHERE f.user_id = :userId
                      AND (:q IS NULL OR LOWER(CONCAT_WS(' ', COALESCE(f.title,''), COALESCE(f.category,''), COALESCE(f.tags,''), COALESCE(f.original_filename,''))) LIKE LOWER(CONCAT('%', :q, '%')))
                      AND (:category IS NULL OR LOWER(f.category) = LOWER(:category))
                      AND (:tag IS NULL OR LOWER(f.tags) LIKE LOWER(CONCAT('%', :tag, '%')))
                    """,
            nativeQuery = true
    )
    Page<FileUrlEntity> search(
            @Param("userId") Long userId,
            @Param("q") String q,
            @Param("category") String category,
            @Param("tag") String tag,
            Pageable pageable
    );

    void deleteByPhotoId(String photoId);

    void deleteByPhotoIdAndUserId(String photoId, Long userId);
}

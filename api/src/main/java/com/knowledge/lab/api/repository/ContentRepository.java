package com.knowledge.lab.api.repository;

import com.knowledge.lab.api.model.Content;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ContentRepository extends MongoRepository<Content, String> {

    Page<Content> findByShelfIdAndStatus(String shelfId, Content.ContentStatus status, Pageable pageable);

    /** Owner sees all their content regardless of status. */
    Page<Content> findByUploaderId(String uploaderId, Pageable pageable);

    Page<Content> findByUploaderIdAndStatus(String uploaderId, Content.ContentStatus status, Pageable pageable);

    Page<Content> findByStatus(Content.ContentStatus status, Pageable pageable);

    List<Content> findByShelfId(String shelfId);

    long countByShelfId(String shelfId);

    long countByUploaderId(String uploaderId);
}

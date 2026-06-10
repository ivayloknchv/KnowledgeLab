package com.knowledge.lab.api.repository;

import com.knowledge.lab.api.model.Annotation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface AnnotationRepository extends MongoRepository<Annotation, String> {

    /** All PUBLIC annotations on a content item. */
    Page<Annotation> findByContentIdAndIsPublicTrue(String contentId, Pageable pageable);

    /** Author's own annotations on a content item (any visibility). */
    List<Annotation> findByContentIdAndAuthorId(String contentId, String authorId);

    /** All annotations on a page that the requester can see (own + public). */
    @Query("{ 'contentId': ?0, 'pageNumber': ?1, $or: [ { 'authorId': ?2 }, { 'isPublic': true } ] }")
    List<Annotation> findVisibleByContentIdAndPageNumber(String contentId, int pageNumber, String requesterId);

    /** All annotations on a content item that the requester can see (own + public). */
    @Query("{ 'contentId': ?0, $or: [ { 'authorId': ?1 }, { 'isPublic': true } ] }")
    Page<Annotation> findVisibleByContentId(String contentId, String requesterId, Pageable pageable);

    long countByContentId(String contentId);

    long countByAuthorId(String authorId);

    void deleteByContentId(String contentId);
}

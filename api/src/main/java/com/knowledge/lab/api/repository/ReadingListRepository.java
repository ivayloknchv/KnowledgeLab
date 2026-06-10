package com.knowledge.lab.api.repository;

import com.knowledge.lab.api.model.ReadingList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReadingListRepository extends MongoRepository<ReadingList, String> {

    List<ReadingList> findByOwnerId(String ownerId);

    Optional<ReadingList> findByOwnerIdAndName(String ownerId, String name);

    boolean existsByOwnerIdAndName(String ownerId, String name);

    /** Find all lists containing a specific contentId. */
    @Query("{ 'ownerId': ?0, 'entries.contentId': ?1 }")
    List<ReadingList> findByOwnerIdAndEntriesContentId(String ownerId, String contentId);

    Page<ReadingList> findByIsPublicTrue(Pageable pageable);
}

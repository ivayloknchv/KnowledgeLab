package com.knowledge.lab.api.repository;

import com.knowledge.lab.api.model.Shelf;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ShelfRepository extends MongoRepository<Shelf, String> {

    Page<Shelf> findByIsPublicTrue(Pageable pageable);

    List<Shelf> findByOwnerIdIsNull();   // system/global shelves

    List<Shelf> findByOwnerId(String ownerId);

    boolean existsByOwnerIdAndName(String ownerId, String name);
}

package com.knowledge.lab.api.repository;

import com.knowledge.lab.api.model.ContentDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ContentSearchRepository extends ElasticsearchRepository<ContentDocument, String> {
    // Custom queries are executed via ElasticsearchOperations in ContentSearchService
}

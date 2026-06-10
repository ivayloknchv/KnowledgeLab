package com.knowledge.lab.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch repository scanner.
 *
 * The client itself is fully auto-configured by Spring Boot from application.yaml:
 *   spring.data.elasticsearch.uris
 *   spring.data.elasticsearch.username  (blank → no Authorization header sent)
 *   spring.data.elasticsearch.password
 *   spring.data.elasticsearch.connection-timeout
 *   spring.data.elasticsearch.socket-timeout
 *
 * Do NOT extend ElasticsearchConfiguration here. Doing so registers a second
 * ElasticsearchClient bean that fights with Boot's auto-configured one, and
 * the hand-rolled ClientConfiguration.builder() path does not honour the
 * blank-username guard that Boot's auto-config applies — causing a spurious
 * Authorization header to reach the ES node, which returns HTTP 400 with no
 * body ("Expecting a response body, but none was sent") when
 * xpack.security.enabled=false.
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.knowledge.lab.api.repository")
public class ElasticsearchConfig {
    // Intentionally empty — all client wiring is handled by Spring Boot auto-configuration.
}

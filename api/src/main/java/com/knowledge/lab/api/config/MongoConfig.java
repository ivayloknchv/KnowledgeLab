package com.knowledge.lab.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoAuditing          // Enables @CreatedDate / @LastModifiedDate
@EnableMongoRepositories(basePackages = "com.knowledge.lab.api")
public class MongoConfig {
    // Connection is fully configured via application.yml (MONGODB_URI env var).
    // Add custom converters or indexes here if needed.
}

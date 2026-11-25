package fr.soft_labs.eventbyte.event.infrastructure.search;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "meilisearch")
public record MeilisearchProperties(
        String host,
        String apiKey,
        @DefaultValue("events") String indexName
) {
}
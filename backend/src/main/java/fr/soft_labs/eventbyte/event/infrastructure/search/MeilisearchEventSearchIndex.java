package fr.soft_labs.eventbyte.event.infrastructure.search;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.SearchRequest;
import fr.soft_labs.eventbyte.event.application.port.EventSearchIndex;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class MeilisearchEventSearchIndex implements EventSearchIndex {

    private final Index index;
    private final JsonMapper jsonMapper;

    public MeilisearchEventSearchIndex(MeilisearchProperties properties, JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
        try {
            Client client = new Client(new Config(properties.host(), properties.apiKey()));
            this.index = client.index(properties.indexName());
            index.updateFilterableAttributesSettings(new String[] {"_geo"});
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de configurer l'index Meilisearch", e);
        }
    }

    @Override
    public void index(SearchableEvent event) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("id", event.id());
        document.put("title", event.title());
        document.put("description", event.description());
        if (event.latitude() != null && event.longitude() != null) {
            document.put("_geo", Map.of("lat", event.latitude(), "lng", event.longitude()));
        }
        try {
            index.addDocuments(jsonMapper.writeValueAsString(List.of(document)), "id");
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'indexer l'événement " + event.id(), e);
        }
    }

    @Override
    public void remove(String eventId) {
        try {
            index.deleteDocument(eventId);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de retirer l'événement " + eventId, e);
        }
    }

    @Override
    public List<String> search(String query, double lat, double lng, double radiusKm) {
        String geoFilter = "_geoRadius(%s, %s, %d)".formatted(lat, lng, Math.round(radiusKm * 1000));
        SearchRequest request = SearchRequest.builder()
                .q(query)
                .filter(new String[] {geoFilter})
                .build();
        try {
            return index.search(request).getHits().stream()
                    .map(hit -> String.valueOf(hit.get("id")))
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Recherche Meilisearch impossible", e);
        }
    }
}
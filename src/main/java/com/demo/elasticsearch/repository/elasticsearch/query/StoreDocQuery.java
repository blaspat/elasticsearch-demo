package com.demo.elasticsearch.repository.elasticsearch.query;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.elasticsearch.indices.PutMappingResponse;
import com.demo.elasticsearch.config.elasticsearch.ElasticsearchIndexName;
import com.demo.elasticsearch.model.elasticsearch.doc.StoreDoc;
import com.demo.elasticsearch.repository.elasticsearch.filter.StoreDocFilter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Repository
public class StoreDocQuery {
    private final ElasticsearchClient client;
    private final ElasticsearchIndexName indexName;

    @SneakyThrows
    public void createOrUpdateDocument(StoreDoc doc) {
        IndexResponse response = client.index(i -> i.index(indexName.getStoreIndex()).id(doc.getId()).document(doc));
    }

    @SneakyThrows //only usable if id exists
    public void updateDocument(String id, StoreDoc doc) {
        client.update(u -> u.index(indexName.getStoreIndex()).id(id).doc(doc), StoreDoc.class);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void updateMapping() throws IOException {
        checkCreateIndex();
        PutMappingRequest request = PutMappingRequest.of(builder -> builder
                .index(indexName.getStoreIndex())
                .properties("name", Property.of(p -> p.text(pk -> pk)))
        );

        PutMappingResponse putMappingResponse = client.indices().putMapping(request);
        log.info("Update mapping index {} : acknowledge {}", indexName.getStoreIndex(), putMappingResponse.acknowledged());
    }

    @SneakyThrows
    private void checkCreateIndex() {
        if (client.indices().exists(e -> e.index(indexName.getStoreIndex())).value()) {
            return;
        }

        CreateIndexResponse createIndexResponse = client.indices().create(builder -> builder
                .index(indexName.getStoreIndex())
                .mappings(TypeMapping.of(type -> type
                        .properties("code", Property.of(p -> p.keyword(pk -> pk)))
                ))
        );
        log.info("Success creating index {} : {}", indexName.getStoreIndex(), createIndexResponse.acknowledged());
    }

    @SneakyThrows
    public List<StoreDoc> searchWithFilter(String code, String name) {
        SearchRequest searchRequest = SearchRequest.of(sr -> sr.index(indexName.getStoreIndex())
                .query(q -> q.bool(qb -> {
                    List<Query> queries = new ArrayList<>();
                    queries.add(StoreDocFilter.filterByCode(code));
                    queries.add(StoreDocFilter.filterByName(name));

                    return qb.filter(queries);
                }))
        );

        SearchResponse<StoreDoc> searchResponse = client.search(searchRequest, StoreDoc.class);

        return searchResponse.hits().hits().stream().map(Hit::source).toList();
    }

    @SneakyThrows
    public StoreDoc getDocumentById(String id) {
        GetResponse<StoreDoc> response = client.get(g -> g.index(indexName.getStoreIndex()).id(id), StoreDoc.class);
        if (response.found()) {
            return response.source();
        } else {
            return null;
        }
    }
}

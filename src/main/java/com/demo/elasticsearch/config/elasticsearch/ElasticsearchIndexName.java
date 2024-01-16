package com.demo.elasticsearch.config.elasticsearch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchIndexName {
    @Value(value = "${elasticsearch.index.store-name}")
    private String STORE_INDEX_NAME;

    public String getStoreIndex() {
        return STORE_INDEX_NAME;
    }
}

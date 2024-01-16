package com.demo.elasticsearch.repository.elasticsearch.filter;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.apache.commons.lang3.StringUtils;

public class StoreDocFilter {
    public static Query filterByCode(String code) {
        if (StringUtils.isBlank(code)) return Query.of(q -> q.bool(qb -> qb));
        return Query.of(q -> q.term(t -> t.field("code").value(code)));
    }

    public static Query filterByName(String name) {
        if (StringUtils.isBlank(name)) return Query.of(q -> q.bool(qb -> qb));
        return Query.of(q -> q.match(t -> t.field("name").query(name)));
    }
}

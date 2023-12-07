package com.demo.elasticsearch.model.elasticsearch.doc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class StoreDoc {
    @Id
    private String id;
    private String code;
    private String name;
}

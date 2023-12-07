package com.demo.elasticsearch.config;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> general(Exception e) {
        log.error("Error {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }

    @ExceptionHandler(ElasticsearchException.class)
    public ResponseEntity<?> elasticsearchException(ElasticsearchException e) {
        log.error(e.error().toString(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Elasticsearch error " + e.error().reason());
    }
}

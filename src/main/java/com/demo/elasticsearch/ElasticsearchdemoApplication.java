package com.demo.elasticsearch;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
@OpenAPIDefinition(
        servers = {
                @Server(url = "/", description = "Local Server URL")
        }
)
public class ElasticsearchdemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElasticsearchdemoApplication.class, args);
    }

}

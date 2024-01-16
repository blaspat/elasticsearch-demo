package com.demo.elasticsearch.config.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.NodeSelector;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.net.ssl.SSLContext;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;

@Configuration
@EnableScheduling
public class ElasticsearchClientConfig implements FactoryBean<ElasticsearchClient>, InitializingBean, DisposableBean {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${elasticsearch.host}")
    private String hosts;

    @Value("${elasticsearch.scheme}")
    private String scheme;

    @Value("${elasticsearch.username}")
    private String username;

    @Value("${elasticsearch.password}")
    private String pwd;

    private ElasticsearchClient client;
    private RestClient lowLevelClient;
    protected static boolean clientConnected = true;

    private SSLContext getSSLContext() throws Exception {
        SSLContextBuilder builder = SSLContexts.custom();
        builder.loadTrustMaterial(null, (x509Certificates, s) -> true);
        return builder.build();
    }

    @Override
    public void destroy() {
        try {
            log.info("Closing Elasticsearch Low Level client");
            if (lowLevelClient != null) {
                lowLevelClient.close();
                clientConnected = false;
            }
        } catch (final Exception e) {
            log.error("Error closing Elasticsearch Low Level client: ", e);
        }
    }

    @Override
    public ElasticsearchClient getObject() {
        return getClient();
    }

    public ElasticsearchClient getClient() {
        if (!clientConnected) {
            try {
                return constructClient();
            } catch (Exception e1) {
                log.error("Failed construct Elasticsearch client", e1);
                throw new RuntimeException(e1);
            }
        } else {
            return client;
        }
    }

    @Override
    public Class<?> getObjectType() {
        return ElasticsearchClient.class;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        constructClient();
    }

    private ElasticsearchClient constructClient() throws Exception{
        log.info("Starting Elasticsearch Low Level client");
        lowLevelClient = buildElasticsearchLowLevelClient();
        // Create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(lowLevelClient, new JacksonJsonpMapper());
        // And create the API client
        log.info("Starting Elasticsearch client");
        client = new ElasticsearchClient(transport);
        clientConnected = true;
        return client;
    }

    private RestClient buildElasticsearchLowLevelClient() throws Exception {
        final SSLContext sslContext = getSSLContext();

        String[] httpHostUrlArr = StringUtils.split(hosts, ",");
        HttpHost[] httpHostArr = new HttpHost[httpHostUrlArr.length];
        for (int i = 0; i < httpHostUrlArr.length; i++) {
            String host = StringUtils.trim(httpHostUrlArr[i]);
            if (StringUtils.isNotBlank(host)) {
                String[] split = host.split(":");
                String esHost = split[0];
                int esPort = 9200;
                if (split.length == 1) {
                    log.warn("No Elasticsearch port found for host {}, automatically use default port 9200", esHost);
                } else {
                    esPort = Integer.parseInt(split[1]);
                }

                httpHostArr[i] = new HttpHost(esHost, esPort, scheme);
            }
        }

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, pwd));
        return RestClient.builder(httpHostArr)
                .setCompressionEnabled(true)
                .setHttpClientConfigCallback(httpAsyncClientBuilder -> httpAsyncClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider)
                        .setSSLContext(sslContext)
                        .setSSLHostnameVerifier((hostname, session) -> true)
                        .setDefaultIOReactorConfig(IOReactorConfig.custom().setSoKeepAlive(true).build())
                )
                .setNodeSelector(NodeSelector.SKIP_DEDICATED_MASTERS)
                .build();
    }

    @Scheduled(fixedDelay = 60000)
    private void checkConnection() {
        try {
            client.ping();
            clientConnected = true;
        } catch (Exception e) {
            log.warn("Failed ping Elasticsearch client, set clientConnected to false");
            clientConnected = false;
        }
    }

    @Component
    @Slf4j
    @RestControllerAdvice
    public static class ElasticsearchExceptionHandler  {
        @ExceptionHandler(CancellationException.class)
        public ResponseEntity<?> cancellationException(CancellationException e) {
            clientConnected = false;

            Map<String, Object> response = new HashMap<>();
            response.put("status", "03");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

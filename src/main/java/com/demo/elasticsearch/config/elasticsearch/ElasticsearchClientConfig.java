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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;

@Slf4j
@Configuration
public class ElasticsearchClientConfig implements FactoryBean<ElasticsearchClient>, InitializingBean, DisposableBean {

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

    private SSLContext getSSLContext() throws Exception {
        SSLContextBuilder builder = SSLContexts.custom();
//        builder.loadTrustMaterial(generateKeyStoreFile(), keyStorePass.toCharArray(), new TrustSelfSignedStrategy());
        // ignore PKIX error
        builder.loadTrustMaterial(null, (x509Certificates, s) -> true);
        return builder.build();
    }

    @Override
    public void destroy() {
        try {
            log.info("Closing Elasticsearch Low Level client");
            if (lowLevelClient != null) {
                lowLevelClient.close();
            }
        } catch (final Exception e) {
            log.error("Error closing Elasticsearch Low Level client: ", e);
        }
    }

    @Override
    public ElasticsearchClient getObject() {
        return client;
    }

    @Override
    public Class<?> getObjectType() {
        return ElasticsearchClient.class;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Starting Elasticsearch Low Level client");
        lowLevelClient = buildElasticsearchLowLevelClient();
        // Create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(lowLevelClient, new JacksonJsonpMapper());
        // And create the API client
        log.info("Starting Elasticsearch client");
        client = new ElasticsearchClient(transport);
    }

    private RestClient buildElasticsearchLowLevelClient() throws Exception {
        final SSLContext sslContext = getSSLContext();

        String[] httpHostUrlArr = StringUtils.split(hosts, ",");
        HttpHost[] httpHostArr = new HttpHost[httpHostUrlArr.length];
        for (int i = 0; i < httpHostUrlArr.length; i++) {
            String host = StringUtils.trim(httpHostUrlArr[i]);
            if (StringUtils.isNotBlank(host)) {
                httpHostArr[i] = new HttpHost(
                        host.split(":")[0],
                        Integer.parseInt(host.split(":")[1]),
                        scheme);
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
}

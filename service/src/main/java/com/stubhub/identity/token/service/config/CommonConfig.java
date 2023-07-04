package com.stubhub.identity.token.service.config;

import com.stubhub.identity.token.service.dye.DyeInterceptor;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CommonConfig {

  // Determines the timeout in milliseconds until a connection is established.
  private static final int CONNECT_TIMEOUT = 10000;

  // The timeout when requesting a connection from the connection manager.
  private static final int CONNECTION_REQUEST_TIMEOUT = 10000;

  // The timeout for waiting for data
  private static final int SOCKET_TIMEOUT = 15000;

  private static final int MAX_TOTAL_CONNECTIONS = 2000;

  private static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 800;

  @Bean
  public ModelMapper modelMapper() {
    return new ModelMapper();
  }

  @Bean
  public RestTemplate shapeTemplate()
      throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
    SSLContext sslContext =
        SSLContextBuilder.create().loadTrustMaterial(new TrustAllStrategy()).build();

    HostnameVerifier allowAllHosts = new NoopHostnameVerifier();
    SSLConnectionSocketFactory connectionFactory =
        new SSLConnectionSocketFactory(sslContext, allowAllHosts);

    PoolingHttpClientConnectionManager poolingConnectionManager = new PoolingHttpClientConnectionManager();
    poolingConnectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
    poolingConnectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_CONNECTIONS_PER_ROUTE);


    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT)
        .setConnectTimeout(CONNECT_TIMEOUT)
        .setSocketTimeout(SOCKET_TIMEOUT).build();

    CloseableHttpClient httpClient =
        HttpClients.custom()
            .setSSLSocketFactory(connectionFactory)
            .setConnectionManager(poolingConnectionManager)
            .setDefaultRequestConfig(requestConfig)
            .build();

    HttpComponentsClientHttpRequestFactory requestFactory =
        new HttpComponentsClientHttpRequestFactory();

    requestFactory.setHttpClient(httpClient);

    RestTemplate shapeRestTemplate = new RestTemplate(requestFactory);

    shapeRestTemplate.getInterceptors().add(new DyeInterceptor());
    return shapeRestTemplate;
  }

  @Bean
  public RestTemplate restTemplate() {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.getInterceptors().add(new DyeInterceptor());
    return restTemplate;
  }
}

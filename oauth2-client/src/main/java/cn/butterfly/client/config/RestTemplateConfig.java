package cn.butterfly.client.config;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * @Author Wuxianda
 * @Date 2023/4/11 16:09
 * @Version 1.0
 * 添加配置后去掉原来RestTemplateUtil的httpclient配置，否则配置不生效
 */
@Configuration
public class RestTemplateConfig {

    @Value("${ar.http.max-connections}")
    String maxConnections;

    @Value("${ar.http.default-per-route}")
    String defaultPerRoute;

    @Value("${ar.http.read-timeout}")
    String readTimeout;

    @Value("${ar.http.connect-timeout}")
    String connectTimeout;

    @Value("${ar.http.connection-request-timeout}")
    String connectionRequestTimeout;

    @Value("${ar.http.connection-keep-alive-time}")
    String keepAliveTime;

    private static final int DEFAULT_KEEP_ALIVE_TIME_MILLIS = 10 * 1000;
    @Bean
    public HttpClientConnectionManager poolingConnectionManager() {
        PoolingHttpClientConnectionManager poolingConnectionManager = new PoolingHttpClientConnectionManager(30, TimeUnit.SECONDS);
        // 连接池最大连接数
        poolingConnectionManager.setMaxTotal(Integer.parseInt(maxConnections));
        // 每个路由的最大连接数,如果只调用一个地址,可以将其设置为最大连接数
        poolingConnectionManager.setDefaultMaxPerRoute(Integer.parseInt(defaultPerRoute));
        return poolingConnectionManager;
    }

    @Bean
    public HttpClientBuilder httpClientBuilder() {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        //设置HTTP连接管理器
        httpClientBuilder.setConnectionManager(poolingConnectionManager());
        httpClientBuilder.setKeepAliveStrategy(connectionKeepAliveStrategy());
        httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(3, true));
        return httpClientBuilder;
    }

    @Bean
    public ConnectionKeepAliveStrategy connectionKeepAliveStrategy() {
        return (response, context) -> {
            HeaderElementIterator it = new BasicHeaderElementIterator
                    (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();

                if (value != null && param.equalsIgnoreCase("timeout")) {
                    return Long.parseLong(value) * 1000;
                }
            }
            return Integer.parseInt(keepAliveTime);
        };
    }

    @Bean("restTemplate")
    public RestTemplate restTemplate() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
//        httpRequestFactory.setHttpClient(httpClientBuilder().build());
        httpRequestFactory.setConnectionRequestTimeout(Integer.parseInt(connectionRequestTimeout));
        httpRequestFactory.setConnectTimeout(Integer.parseInt(connectTimeout));
        httpRequestFactory.setReadTimeout(Integer.parseInt(readTimeout));
        RestTemplate restTemplate = new RestTemplate(generateHttpRequestFactory());
        //设置编码
        restTemplate.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }

    private HttpComponentsClientHttpRequestFactory generateHttpRequestFactory() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        TrustStrategy acceptingTrustStrategy = (x509Certificates, authType) -> true;
        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        SSLConnectionSocketFactory connectionSocketFactory = new SSLConnectionSocketFactory(sslContext,
                new NoopHostnameVerifier());

        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        httpClientBuilder.setSSLSocketFactory(connectionSocketFactory);
        httpClientBuilder.setConnectionManager(poolingConnectionManager());
        httpClientBuilder.setKeepAliveStrategy(connectionKeepAliveStrategy());
        httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(3, true));
        CloseableHttpClient httpClient = httpClientBuilder.build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);
        return factory;
    }
}

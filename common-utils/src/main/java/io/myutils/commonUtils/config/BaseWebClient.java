package io.myutils.commonUtils.config;

import io.myutils.commonUtils.model.webClient.DelegateParams;
import io.myutils.commonUtils.model.webClient.WebClientConfig;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider;
import reactor.netty.transport.ProxyProvider;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Getter
@Setter
@Component
public class BaseWebClient {
    private Map<String, WebClient> webClientMap;
    private BaseWebClient(){}
    @Autowired
    WebClientConfigMap webClientConfigMap;

    @Autowired
    public BaseWebClient(WebClientConfigMap webClientConfigMap) {
        webClientMap = new HashMap<>();
        webClientConfigMap.webClientConfigMapMap().entrySet()
                .stream()
                .filter((p -> !p.getValue().sslWebClientFlag()))
                .forEach((entry) -> {
                    webClientMap.put(entry.getKey(), buildWebclient(entry.getKey(), entry.getValue()));
                });
    }

    public WebClient buildWebclient(String connectionPoolName, WebClientConfig webClientConfig) {
        ExchangeStrategies exchangeStrategies =
                ExchangeStrategies.builder()
                        .codecs(
                                configure -> configure.defaultCodecs()
                                        .maxInMemorySize(1024 * 1024 * webClientConfig.memoryLimit())
                        ).build();
        return WebClient.builder()
                .clientConnector(getHttpClient(webClientConfig, connectionPoolName))
                .exchangeStrategies(exchangeStrategies)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void buildSslWebClientWithParams(String connectionPoolName,
                                            String baseUrl,
                                            PrivateKey privateKey,
                                            String keyPassword,
                                            X509Certificate[] keyCertChain,
                                            X509Certificate[] trustCertCollection){
        WebClientConfig webClientConfig = webClientConfigMap
                .webClientConfigMapMap().get(connectionPoolName);

        ExchangeStrategies exchangeStrategies =
                ExchangeStrategies.builder()
                        .codecs(
                                configure -> configure.defaultCodecs()
                                        .maxInMemorySize(1024 * 1024 * webClientConfig.memoryLimit())
                        ).build();

        WebClient webClient =  WebClient.builder()
                .filters(exchangeFilterFunctions -> exchangeFilterFunctions
                        .add(logRequestHeaders()))
                .clientConnector(getHttpClient(connectionPoolName, webClientConfig, baseUrl, privateKey, keyPassword, keyCertChain, trustCertCollection))
                .baseUrl(baseUrl)
                .exchangeStrategies(exchangeStrategies)
                .build();

        if (webClientMap.isEmpty()){
            webClientMap = new HashMap<>();
        }
        webClientMap.put(connectionPoolName, webClient);
    }

    private ReactorClientHttpConnector getHttpClient(String connectionPoolName,
                                                     WebClientConfig webClientConfig,
                                                     String baseUrl,
                                                     PrivateKey privateKey,
                                                     String keyPassword,
                                                     X509Certificate[] keyCertChain,
                                                     X509Certificate[] trustCertCollection){
        SslProvider sslProvider = SslProvider.builder()
                        .sslContext(buildSslContextReactorClientHttpConnector(privateKey, keyPassword, keyCertChain, trustCertCollection))
                        .build();
        ConnectionProvider connProvider = ConnectionProvider
                .builder(connectionPoolName)
                .maxIdleTime(Duration.ofMillis(webClientConfig.maxIdleTime()))
                .maxLifeTime(Duration.ofMillis(webClientConfig.maxLifetime()))
                .maxConnections(webClientConfig.maxConnections())
                .pendingAcquireTimeout(Duration.ofMillis(webClientConfig.pendingAcquireTimeout()))
                .evictInBackground(Duration.ofMillis(webClientConfig.evictinBackground()))
                .build();

        HttpClient httpClient =
                HttpClient.create(connProvider)
                        .secure(sslProvider)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, webClientConfig.connectionTimeout())
                        .doOnConnected(conn ->
                                conn.addHandlerFirst(new ReadTimeoutHandler(webClientConfig.readTimeout(), TimeUnit.MILLISECONDS))
                                        .addHandlerFirst(new WriteTimeoutHandler(webClientConfig.writeTimeout(),TimeUnit.MILLISECONDS))
                        );

    return new ReactorClientHttpConnector(httpClient);
    }

    private SslContext buildSslContextReactorClientHttpConnector(PrivateKey privateKey,
                                                                 String keyPassword,
                                                                 X509Certificate[] keyCertChain,
                                                                 X509Certificate[] trustCertCollection){
        SslContext sslContext = null;
        try{
            sslContext =
                    SslContextBuilder.forClient()
                            .keyManager(privateKey, keyPassword, keyCertChain)
                            .trustManager(trustCertCollection)
                            .build();
        } catch (Exception e){
            //Add log
        }
        return sslContext;
    }

    private ReactorClientHttpConnector getHttpClient(WebClientConfig webClientConfig, String connectionPoolName) {
        ConnectionProvider connProvider = ConnectionProvider
                .builder(connectionPoolName)
                .maxIdleTime(Duration.ofMillis(webClientConfig.maxIdleTime()))
                .maxLifeTime(Duration.ofMillis(webClientConfig.maxLifetime()))
                .maxConnections(webClientConfig.maxConnections())
                .pendingAcquireTimeout(Duration.ofMillis(webClientConfig.pendingAcquireTimeout()))
                .evictInBackground(Duration.ofMillis(webClientConfig.evictinBackground()))
                .build();
        // add Logs
        HttpClient httpClient = null;
        if (webClientConfig.usingProxy() && !ObjectUtils.isEmpty(webClientConfig.proxyDetails())){
            httpClient =
                    HttpClient.create(connProvider)
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, webClientConfig.connectionTimeout())
                            .proxy(proxySpec -> proxySpec.type(ProxyProvider.Proxy.HTTP)
                                    .host(webClientConfig.proxyDetails().proxyHost())
                                    .port(webClientConfig.proxyDetails().proxyPort())
                                    .build()
                            )
                            .doOnConnected(conn ->
                                    conn.addHandlerFirst(new ReadTimeoutHandler(webClientConfig.readTimeout(), TimeUnit.MILLISECONDS))
                                            .addHandlerFirst(new WriteTimeoutHandler(webClientConfig.writeTimeout(),TimeUnit.MILLISECONDS))
                                    );
        } else {
            httpClient =
                    HttpClient.create(connProvider)
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, webClientConfig.connectionTimeout())
                            .doOnConnected(conn ->
                                    conn.addHandlerFirst(new ReadTimeoutHandler(webClientConfig.readTimeout(), TimeUnit.MILLISECONDS))
                                            .addHandlerFirst(new WriteTimeoutHandler(webClientConfig.writeTimeout(),TimeUnit.MILLISECONDS))
                            );
        }
        return new ReactorClientHttpConnector(httpClient);
    }

    /*
    * To Invoke endpoints of HTTP GET method
    * @param delegateParams
    * @return Object
    * */
    public Object invokeGet(DelegateParams delegateParams){
        AtomicInteger retryCounter = new AtomicInteger(0);
        Instant start = Instant.now();
        WebClient.ResponseSpec responseSpec = webClientMap.get(delegateParams.connectionPoolName())
                .get()
                .uri(delegateParams.uri())
                .headers(h  -> h.addAll(delegateParams.headers()))
                .retrieve();
        return extractResponse(delegateParams, responseSpec, retryCounter, start);
    }
    /*
    * To Invoke endpoints of HTTP DELETE method
    * @param delegateParams
    * @return Object
    * */
    public Object invokeDelete(DelegateParams delegateParams){
        AtomicInteger retryCounter = new AtomicInteger(0);
        Instant start = Instant.now();
        WebClient.ResponseSpec responseSpec = webClientMap.get(delegateParams.connectionPoolName())
                .delete()
                .uri(delegateParams.uri())
                .headers(h  -> h.addAll(delegateParams.headers()))
                .retrieve();
        return extractResponse(delegateParams, responseSpec, retryCounter, start);
    }
    /*
    * To Invoke endpoints of HTTP POST method
    * @param delegateParams
    * @return Object
    * */
    public Object invokePost(DelegateParams delegateParams){
        AtomicInteger retryCounter = new AtomicInteger(0);
        Instant start = Instant.now();
        WebClient.ResponseSpec responseSpec = webClientMap.get(delegateParams.connectionPoolName())
                .post()
                .uri(delegateParams.uri())
                .headers(h  -> h.addAll(delegateParams.headers()))
                .bodyValue(delegateParams.requestBody())
                .retrieve();
        return extractResponse(delegateParams, responseSpec, retryCounter, start);
    }
    /*
    * To Invoke endpoints of HTTP PUT method
    * @param delegateParams
    * @return Object
    * */
    public Object invokePut(DelegateParams delegateParams){
        AtomicInteger retryCounter = new AtomicInteger(0);
        Instant start = Instant.now();
        WebClient.ResponseSpec responseSpec = webClientMap.get(delegateParams.connectionPoolName())
                .get()
                .uri(delegateParams.uri())
                .headers(h  -> h.addAll(delegateParams.headers()))
                .retrieve();
        return extractResponse(delegateParams, responseSpec, retryCounter, start);
    }

    private Object extractResponse(DelegateParams delegateParams, WebClient.ResponseSpec responseSpec, AtomicInteger retryCounter, Instant start) {
        return delegateParams.toResponseEntity()
                ? getResponseEntityAsResponse(delegateParams, responseSpec, retryCounter, start)
                : getMonoObjAsResponse(delegateParams, responseSpec, retryCounter, start);
    }

    private Object getResponseEntityAsResponse(DelegateParams delegateParams, WebClient.ResponseSpec responseSpec, AtomicInteger retryCounter, Instant start) {
        Object resObj;
        WebClientConfig webClientConfig = webClientConfigMap
                .webClientConfigMapMap()
                .get(delegateParams.connectionPoolName());
        resObj = responseSpec.toEntity(delegateParams.parameterizedTypeReference())
                .retryWhen(Retry.fixedDelay(webClientConfig.maxAttempts(), Duration.ofMillis(webClientConfig.fixedDelayDuration()))
                        .filter(e -> predicateRetry(e, retryCounter.get(), webClientConfig.usingProxy(), webClientConfig))
                        .doAfterRetry(retrySignal -> retryCounter.incrementAndGet())
                        .onRetryExhaustedThrow(
                                ((retryBackoffSpec, retrySignal) -> {
                                    try {
                                        throw new Exception();
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                        )).doOnSuccess(
                                clientResponse -> logRetrySuccess(delegateParams.uri(), retryCounter.get(), start)
                )
                .block();
        return resObj;
    }

    private Object getMonoObjAsResponse(DelegateParams delegateParams, WebClient.ResponseSpec responseSpec, AtomicInteger retryCounter, Instant start) {
        Object resObj;
        WebClientConfig webClientConfig = webClientConfigMap
                .webClientConfigMapMap()
                .get(delegateParams.connectionPoolName());
        resObj = responseSpec.bodyToMono(delegateParams.parameterizedTypeReference())
                .retryWhen(Retry.fixedDelay(webClientConfig.maxAttempts(), Duration.ofMillis(webClientConfig.fixedDelayDuration()))
                        .filter(e -> predicateRetry(e, retryCounter.get(), webClientConfig.usingProxy(), webClientConfig))
                        .doAfterRetry(retrySignal -> retryCounter.incrementAndGet())
                        .onRetryExhaustedThrow(
                                ((retryBackoffSpec, retrySignal) -> {
                                    try {
                                        throw new Exception();
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                        )).doOnSuccess(
                        clientResponse -> logRetrySuccess(delegateParams.uri(), retryCounter.get(), start)
                )
                .block();
        return resObj;
    }

    private void logRetrySuccess(String uri, int retryCounter, Instant start) {
        Long timElapsed = Duration.between(start, Instant.now()).toMillis();
        // add log
    }

    private boolean predicateRetry(Throwable e, int retryCounter, boolean usingProxy, WebClientConfig webClientConfig) {
        boolean retry = false;
        List<Integer> errCodesArr = Stream.of(webClientConfig.retryErrorCodesStr().split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .toList();
        if (e instanceof WebClientResponseException wre) {
            int errorCode = wre.getStatusCode().value();
            retry = errCodesArr.contains(errorCode);
            // add log
        } else if (e instanceof ResourceAccessException && e.getCause() != null
                && (e.getCause() instanceof ConnectException || e.getCause() instanceof SocketTimeoutException)
        ) {
            retry = true;
            // add log
        } else{
            retry =false;
            // add log
        }
        return retry;
    }
    private ExchangeFilterFunction logRequestHeaders() {
        // Add request
        return ExchangeFilterFunction.ofRequestProcessor(
                Mono::just
        );
    }

}

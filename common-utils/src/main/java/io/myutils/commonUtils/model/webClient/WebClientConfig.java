package io.myutils.commonUtils.model.webClient;


import jakarta.validation.constraints.NotEmpty;

public record WebClientConfig(
        @NotEmpty int memoryLimit,
        @NotEmpty int readTimeout,
        @NotEmpty int writeTimeout,
        @NotEmpty int connectionTimeout,
        @NotEmpty int maxIdleTime,
        @NotEmpty int maxLifetime,
        @NotEmpty int pendingAcquireTimeout,
        @NotEmpty int maxConnections,
        @NotEmpty int maxAttempts,
        @NotEmpty int fixedDelayDuration,
        @NotEmpty int evictinBackground,
        @NotEmpty String serviceName,
        @NotEmpty String retryErrorCodesStr,
        boolean sslWebClientFlag,
        boolean usingProxy,
        ProxyDetails proxyDetails) {
}

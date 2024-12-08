package io.myutils.commonUtils.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

@ConfigurationProperties(prefix = "caching.servicecache")
@Getter
@Builder
@AllArgsConstructor
public class ServiceCacheProperties {

    private final RedisProperties redis;
    private final CaffeineProperties caffeine;

    @Bean("serviceCacheProperties")
    public  CacheProperties configPropertiesDto() { return new CacheProperties(redis, caffeine);}
}

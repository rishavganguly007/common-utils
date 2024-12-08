package io.myutils.commonUtils.cache;


import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@EnableConfigurationProperties(ServiceCacheProperties.class)
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "caching.servicecahe.redis")
public class ServiceCacheConfig extends AbstractCompositeRedisCacheConfig{

    @Autowired
    @Qualifier("serviceCacheProperties")
    CacheProperties cacheProperties;

    @Override
    CacheProperties getCacheProperties() {
        return cacheProperties;
    }

    @Override
    @RefreshScope
    @Bean("serviceConnectionFactory")
    @ConditionalOnProperty( name = "caching.servicecache.redis.enabled", havingValue = "true", matchIfMissing = false)
    public LettuceConnectionFactory connectionFactory() { return super.connectionFactory(); }

    @Override
    @RefreshScope
    @Bean("serviceRedisTemplate")
    @ConditionalOnProperty( name = "caching.servicecache.redis.enabled", havingValue = "true", matchIfMissing = false)
    public RedisTemplate<String, Object> redisTemplate(
            @Qualifier("serviceConnectionFactory")
            RedisConnectionFactory connectionFactory
    ) {
        return super.redisTemplate(connectionFactory);
    }
    @Override
    @RefreshScope
    @Bean("serviceFallbackCacheManager")
    @ConditionalOnProperty( name = "caching.servicecache.redis.enabled", havingValue = "true", matchIfMissing = false)
    public FallbackCacheManager fallbackCacheManager(
                    @Qualifier("serviceL1CacheManager")
                    @Nullable
                    CaffeineCacheManager l1CacheManager,

                    @Qualifier("serviceL1CacheManager")
                    @Nullable
                    RedisCacheManager l2CacheManager
    ) {
        return super.fallbackCacheManager(l1CacheManager, l2CacheManager);
    }

    /*
    * caffeine is the Primary cache
    * */
    @Override
    @Primary
    @RefreshScope
    @Bean("serviceL1CacheManager")
    public CaffeineCacheManager caffeineCacheManager() { return super.caffeineCacheManager(); }

    @Override
    @RefreshScope
    @Bean("serviceL2DistributedCacheManager")
    @ConditionalOnProperty( name = "caching.servicecache.redis.enabled", havingValue = "true", matchIfMissing = false)
    public RedisCacheManager redisCacheManager(
            @Qualifier("serviceConnectionFactory")
            RedisConnectionFactory connectionFactory
    ) { return super.redisCacheManager(connectionFactory); }

}

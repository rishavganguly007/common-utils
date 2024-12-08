package io.myutils.commonUtils.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class AbstractCompositeRedisCacheConfig {
    abstract CacheProperties getCacheProperties();

    public String getRedisUserName(){
        return Optional.ofNullable(getCacheProperties())
                .map(CacheProperties::getRedis)
                .map(RedisProperties::getUserName)
                .orElse("");
    }
    public int getRedisTtlMinutes(){
        return Optional.ofNullable(getCacheProperties())
                .map(CacheProperties::getRedis)
                .map(RedisProperties::getTimeToLive)
                .orElse(0);
    }

    public String getRedisPort(){
        return Optional.ofNullable(getCacheProperties())
                .map(CacheProperties::getRedis)
                .map(RedisProperties::getPort)
                .orElse("");
    }

    public String getRedisHost(){
        return Optional.ofNullable(getCacheProperties())
                .map(CacheProperties::getRedis)
                .map(RedisProperties::getHost)
                .orElse("");
    }

    public int getRedisReadTimeout(){
        return Optional.ofNullable(getCacheProperties())
                .map(CacheProperties::getRedis)
                .map(RedisProperties::getReadTimeOut)
                .orElse(0);
    }

    public String getRedisPassword(){
        return Optional.ofNullable(getCacheProperties())
                .map(CacheProperties::getRedis)
                .map(RedisProperties::getUserName)
                .orElse("");
    }

    public int getRedisConnectTimeout(){
        return Optional.ofNullable(getCacheProperties())
                .map(CacheProperties::getRedis)
                .map(RedisProperties::getConnectTimeOut)
                .orElse(0);
    }

    public Set<String> getRedisCacheNames() {
        String cacheNames = Optional.ofNullable(getCacheProperties())
                .map(CacheProperties::getRedis)
                .map(RedisProperties::getCacheNames)
                .orElse("");

        return Arrays.stream(cacheNames.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    public boolean isRedisEnabledSsl(){
        return Optional.ofNullable(getCacheProperties())
                .map(CacheProperties::getRedis)
                .map(RedisProperties::isSslEnabled)
                .orElse(false);
    }

    /* Caffeine */
    public Set<String> getCaffeineCacheNames() {
        String cacheNames = Optional.ofNullable(getCacheProperties())
                .map(CacheProperties::getCaffeine)
                .map(CaffeineProperties::getCacheNames)
                .orElse("");

        return Arrays.stream(cacheNames.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    public int getCaffeineTtlSeconds() {
        return Optional.ofNullable(getCacheProperties())
                .map(CacheProperties::getCaffeine)
                .map(CaffeineProperties::getTtlSeconds)
                .orElse(0);
    }

    public int getCaffeineMaxEntries() {
        return Optional.ofNullable(getCacheProperties())
                .map(CacheProperties::getCaffeine)
                .map(CaffeineProperties::getMaxEntries)
                .orElse(0);
    }

    public LettuceConnectionFactory connectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setDatabase(0);
        redisConfig.setHostName(getRedisHost());
        redisConfig.setPort(Integer.parseInt(getRedisPort()));
        redisConfig.setPassword(RedisPassword.of(getRedisPassword()));
        redisConfig.setUsername(getRedisUserName());

        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofMillis(getRedisConnectTimeout()))
                .build();

        ClientOptions clientOptions =
                ClientOptions.builder()
                        .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                        .autoReconnect(true)
                        .socketOptions(socketOptions)
                        .build();

        LettuceClientConfiguration.LettuceClientConfigurationBuilder configurationBuilder =
                LettuceClientConfiguration.builder()
                        .commandTimeout(Duration.ofMillis(getRedisReadTimeout()))
                        .clientOptions(clientOptions);
        if(isRedisEnabledSsl()) {
            configurationBuilder.useSsl();
        }
        LettuceClientConfiguration lettuceClientConfiguration = configurationBuilder.build();

        return new LettuceConnectionFactory(redisConfig, lettuceClientConfiguration);
    }

    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    public FallbackCacheManager fallbackCacheManager(CaffeineCacheManager l1CacheManager,
                                                     RedisCacheManager l2CacheManager) {
        CaffeineCacheManager caffeineUsed = null;
        RedisCacheManager redisUsed = null;
        if (getCacheProperties() != null){
            if (getCacheProperties().getCaffeine() != null && getCacheProperties().getCaffeine().isEnabled()){
                caffeineUsed = l1CacheManager;
            }if (getCacheProperties().getRedis() != null && getCacheProperties().getRedis().isEnabled()){
                redisUsed = l2CacheManager;
            }
        }
        return new FallbackCacheManager(caffeineUsed, redisUsed);
    }

    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCacheNames(getCaffeineCacheNames());
        caffeineCacheManager.setAllowNullValues(false);
        caffeineCacheManager
                .setCaffeine(Caffeine.newBuilder()
                        .expireAfterWrite(getCaffeineTtlSeconds(), TimeUnit.SECONDS)
                        .maximumSize(getCaffeineMaxEntries())
                );
        return caffeineCacheManager;
    }

    public RedisCacheManager redisCacheManager(RedisConnectionFactory connFactory) {
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(getRedisTtlMinutes()))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new SafeJackson2JsonRedisSerializer()));
        return RedisCacheManager.builder(connFactory)
                .cacheDefaults(cacheConfiguration)
                .disableCreateOnMissingCache()
                .initialCacheNames(getRedisCacheNames())
                .build();
    }
}

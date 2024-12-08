package io.myutils.commonUtils.cache;

import lombok.Getter;

@Getter
public class CacheProperties {

    private final RedisProperties redis;

    private final CaffeineProperties caffeine;

    public CacheProperties(RedisProperties redis, CaffeineProperties caffeine){
        this.redis = redis;
        this.caffeine = caffeine;
    }
}

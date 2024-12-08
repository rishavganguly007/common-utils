package io.myutils.commonUtils.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;

import java.util.*;


/**
 * Manages a list of cache managers in search priority order
 * */
public class FallbackCacheManager implements CacheManager {
    private final List<CacheManager> cacheManagers;
    private final List<CacheManager> reversedCacheManagers;
    public FallbackCacheManager(CacheManager ... cacheManagers) {
        this.cacheManagers = Arrays.stream(cacheManagers).filter(Objects::nonNull).toList();
        List<CacheManager> temp = new ArrayList<>();
        for (int i = this.cacheManagers.size() - 1; i >= 0; i--) {
            temp.add(this.cacheManagers.get(i));
        }
        this.reversedCacheManagers = temp;
    }

    public List<CacheManager> getManagersReadOrder() { return cacheManagers; }
    public List<CacheManager> getManagersUpdateOrder() { return reversedCacheManagers; }

    @Override
    public Cache getCache(String name) {
        for (CacheManager cacheManager : this.cacheManagers) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                return cache;
            }
        }
        return null;
    }

    @Override
    public Collection<String> getCacheNames() {
        return cacheManagers.stream()
                .flatMap(cacheManager -> cacheManager.getCacheNames().stream())
                .distinct()
                .toList();
    }
}

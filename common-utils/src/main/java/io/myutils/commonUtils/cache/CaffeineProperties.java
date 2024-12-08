package io.myutils.commonUtils.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CaffeineProperties {

    private int ttlSeconds;
    private int maxEntries;
    private String cacheNames;
    private boolean enabled;
}

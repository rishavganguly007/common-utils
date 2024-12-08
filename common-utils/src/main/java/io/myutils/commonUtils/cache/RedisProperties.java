package io.myutils.commonUtils.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RedisProperties {

    private String userName;
    private String password;
    private String host;
    private String port;
    private boolean sslEnabled;
    private int timeToLive;
    private int connectTimeOut;
    private int readTimeOut;
    private String cacheNames;
    private boolean enabled;
}

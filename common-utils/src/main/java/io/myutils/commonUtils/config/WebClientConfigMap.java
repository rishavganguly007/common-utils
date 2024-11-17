package io.myutils.commonUtils.config;

import io.myutils.commonUtils.model.webClient.WebClientConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix="baseWebClientConfig")
public record WebClientConfigMap(Map<String, WebClientConfig> webClientConfigMapMap) {
}

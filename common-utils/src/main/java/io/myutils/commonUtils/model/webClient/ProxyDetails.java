package io.myutils.commonUtils.model.webClient;

import jakarta.validation.constraints.NotEmpty;

public record ProxyDetails(
        @NotEmpty String proxyHost,
        @NotEmpty int proxyPort
) {
}

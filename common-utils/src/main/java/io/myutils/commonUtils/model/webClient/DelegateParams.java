package io.myutils.commonUtils.model.webClient;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;

public record DelegateParams(
        @NotEmpty String connectionPoolName,
        @NotEmpty String uri,
        @NotEmpty HttpHeaders headers,
        Object requestBody,
        @NotEmpty ParameterizedTypeReference parameterizedTypeReference,
        boolean toResponseEntity
        ) {
}

package com.codeXray.backend.community.dto;

import jakarta.validation.constraints.NotNull;

// value 는 1(추천) 또는 -1(비추천). 0 등 잘못된 값은 서비스에서 걸러 400.
public record VotePostRequest(
        @NotNull Integer value
) {
}

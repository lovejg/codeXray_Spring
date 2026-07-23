package com.codeXray.backend.community.dto;

import jakarta.validation.constraints.NotNull;

public record HidePostRequest(
        @NotNull Boolean hidden
) {
}

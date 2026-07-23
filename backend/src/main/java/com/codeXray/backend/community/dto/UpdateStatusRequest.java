package com.codeXray.backend.community.dto;

import com.codeXray.backend.community.entity.SuggestionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull SuggestionStatus status
) {
}

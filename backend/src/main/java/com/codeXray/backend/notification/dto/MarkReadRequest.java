package com.codeXray.backend.notification.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record MarkReadRequest(
        @NotEmpty List<Long> ids
) {
}

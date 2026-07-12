package com.codeXray.backend.solution.dto;

public record UpsertMemoRequest(
        String wrongReason,
        String logic,
        String keyFunctions,
        String freeNote
) {
}

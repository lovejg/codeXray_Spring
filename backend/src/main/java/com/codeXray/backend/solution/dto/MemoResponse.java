package com.codeXray.backend.solution.dto;

import com.codeXray.backend.solution.entity.Memo;

public record MemoResponse(
        String wrongReason,
        String logic,
        String keyFunctions,
        String freeNote
) {
    public static MemoResponse from(Memo memo) {
        return new MemoResponse(
                memo.getWrongReason(),
                memo.getLogic(),
                memo.getKeyFunctions(),
                memo.getFreeNote()
        );
    }
}

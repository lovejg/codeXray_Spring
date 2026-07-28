package com.codeXray.backend.ai.dto;

import com.codeXray.backend.ai.entity.AiJob;

// AI 잡 상태 응답. 생성 직후엔 status=PENDING/result=null, 완료 후 폴링하면 DONE/result 채워짐.
public record AiJobResponse(
        Long id,
        String kind,
        String status,
        String result,
        String errorCode
) {
    public static AiJobResponse from(AiJob job) {
        return new AiJobResponse(
                job.getId(),
                job.getKind().name(),
                job.getStatus().name(),
                job.getResult(),
                job.getErrorCode()
        );
    }
}

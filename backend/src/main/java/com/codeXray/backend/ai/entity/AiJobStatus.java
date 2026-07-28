package com.codeXray.backend.ai.entity;

public enum AiJobStatus {
    PENDING, // 요청됨(컨슈머 처리 대기)
    DONE,    // 처리 완료(result 있음)
    FAILED   // 처리 실패(error_code 있음)
}

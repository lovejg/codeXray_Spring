package com.codeXray.backend.ai.entity;

public enum AiJobStatus {
    PENDING,    // 요청됨(컨슈머 처리 대기)
    PROCESSING, // 컨슈머 하나가 가져가 Claude 를 호출하는 중. 같은 잡을 두 번 처리하지 않게 하는 표시
    DONE,       // 처리 완료(result 있음)
    FAILED      // 처리 실패(error_code 있음)
}

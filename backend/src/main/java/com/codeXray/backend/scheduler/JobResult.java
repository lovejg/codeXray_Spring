package com.codeXray.backend.scheduler;

// 배치 작업 실행 결과 (성공 여부 + 결과/에러). 수동 트리거 응답에도 사용.
public record JobResult(String job, boolean ok, Object result, String error) {
}

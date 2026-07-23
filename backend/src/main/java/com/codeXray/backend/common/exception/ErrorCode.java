package com.codeXray.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전역 에러 코드.
 * 각 코드는 (HTTP 상태 + 기본 메시지)를 들고 다닌다.
 * 도메인별 코드는 단계 진행하면서 여기에 계속 추가하면 됨 (예: EMAIL_DUPLICATED).
 */
public enum ErrorCode {

    // ── 공통 ──
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),

    // ── 인증/계정 ──
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),

    // ── 이메일 인증 토큰 ──
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 인증 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.BAD_REQUEST, "만료된 인증 토큰입니다. 인증 메일을 다시 요청해 주세요."),
    ALREADY_VERIFIED(HttpStatus.CONFLICT, "이미 인증된 계정입니다."),

    // ── 로그인 ──
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "이메일 인증이 필요합니다. 인증 메일을 확인해 주세요."),

    // ── 계정 관리 ──
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),

    // ── 토큰 재발급 ──
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다. 다시 로그인해 주세요."),

    // ── 풀이 ──
    SOLUTION_NOT_FOUND(HttpStatus.NOT_FOUND, "풀이를 찾을 수 없습니다."),
    PROBLEM_NOT_FOUND(HttpStatus.NOT_FOUND, "문제를 찾을 수 없습니다."),

    // ── 노트 ──
    NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "노트를 찾을 수 없습니다."),

    // ── 커뮤니티 ──
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    PRIVATE_POST(HttpStatus.FORBIDDEN, "비공개 게시글입니다."),

    // ── 커뮤니티 투표 ──
    POST_NOT_VOTABLE(HttpStatus.BAD_REQUEST, "이 글에는 추천/비추천을 할 수 없습니다."),
    CANNOT_VOTE_OWN_POST(HttpStatus.BAD_REQUEST, "본인 글에는 투표할 수 없습니다."),

    // ── 커뮤니티 신고/운영 ──
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다."),
    ALREADY_REPORTED(HttpStatus.BAD_REQUEST, "이미 신고한 게시글입니다."),
    CANNOT_REPORT_OWN_POST(HttpStatus.BAD_REQUEST, "본인 글은 신고할 수 없습니다."),
    NOT_SUGGESTION_POST(HttpStatus.BAD_REQUEST, "건의사항 글에만 가능한 작업입니다."),

    // ── AI ──
    AI_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AI 분석은 하루 한도를 초과했습니다. 내일 다시 시도해 주세요."),
    AI_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI 기능을 현재 사용할 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}

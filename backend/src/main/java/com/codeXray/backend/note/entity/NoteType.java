package com.codeXray.backend.note.entity;

public enum NoteType {
    CODE,    // 코드 템플릿 (BFS, 세그트리 등)
    PATTERN, // 접근 패턴 / 판단 기준
    MISTAKE, // 오답 / 실수 노트
    OTHER    // 기타 (라이브러리 사용법, 치트시트 등)
}

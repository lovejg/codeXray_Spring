package com.codeXray.backend.community.dto;

import com.codeXray.backend.user.entity.User;

// 작성자 표시용. 탈퇴(user=null)면 익명화.
public record AuthorResponse(Long id, String nickname) {

    private static final AuthorResponse ANONYMOUS = new AuthorResponse(0L, "탈퇴한 사용자");

    public static AuthorResponse from(User user) {
        return (user == null) ? ANONYMOUS : new AuthorResponse(user.getId(), user.getNickname());
    }
}

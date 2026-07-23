package com.codeXray.backend.community.dto;

// 게시글의 투표 집계. vote/removeVote 응답이자 목록/상세에 임베드되는 값.
public record VoteSummary(int upvotes, int downvotes, int score, int myVote) {

    private static final VoteSummary EMPTY = new VoteSummary(0, 0, 0, 0);

    public static VoteSummary empty() {
        return EMPTY;
    }

    public static VoteSummary of(int upvotes, int downvotes, int myVote) {
        return new VoteSummary(upvotes, downvotes, upvotes - downvotes, myVote);
    }
}

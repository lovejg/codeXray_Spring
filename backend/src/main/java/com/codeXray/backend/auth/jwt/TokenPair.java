package com.codeXray.backend.auth.jwt;

public record TokenPair(String accessToken, String refreshToken) {
}

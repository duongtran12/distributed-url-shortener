package com.duong.url_shortener.auth;

record AuthSession(LoginResponse loginResponse, String refreshToken) {
}

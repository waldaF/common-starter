package com.company.commonstarter.jwt.client;

public record JwtClientSettings(String secret, int expirationInMinutes, int expirationRefreshBeforeInMinutes) {}
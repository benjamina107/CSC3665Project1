package com.csc3665.project1.dto;

public record LoginResponse(
    int id,
    String name,
    String role,
    boolean isFirstLogin
) {}
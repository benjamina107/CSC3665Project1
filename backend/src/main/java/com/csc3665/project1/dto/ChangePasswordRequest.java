package com.csc3665.project1.dto;
public record ChangePasswordRequest(
    int eid,
    String newPassword
) {}
package com.csc3665.project1.dto;

public record StaffRequest(
    String name,
    String username,
    String password,
    String role
) {}
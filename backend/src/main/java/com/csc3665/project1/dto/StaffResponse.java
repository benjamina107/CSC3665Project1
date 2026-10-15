package com.csc3665.project1.dto;

public record StaffResponse(
    int id,
    String name,
    String username,
    String role
) {}
package com.csc3665.project1.dto;
import java.time.LocalDate;

public record WorkRequest (
    int eid,
    LocalDate startDate,
    LocalDate endDate,
    boolean approved
)
{}

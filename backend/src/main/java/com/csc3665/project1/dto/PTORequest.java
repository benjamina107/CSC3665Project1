package com.csc3665.project1.dto; 

import java.time.LocalDate;

public record PTORequest(
    int eid,
    String reason,
    LocalDate startDate,
    LocalDate endDate
){}

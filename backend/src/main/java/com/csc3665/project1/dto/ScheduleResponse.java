package com.csc3665.project1.dto;

import java.util.List;

// The next two weeks (startDate..endDate inclusive) of assignments for one employee.
// Days with no entry are days off.
public record ScheduleResponse(
    String startDate,
    String endDate,
    List<ScheduleEntry> entries
) {}

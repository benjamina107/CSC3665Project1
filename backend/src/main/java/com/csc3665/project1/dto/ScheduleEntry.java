package com.csc3665.project1.dto;

import java.util.List;

// One assigned shift for one employee. "coworkers" are the opposite-role
// employees working the same shift (doctor -> technicians, technician -> doctors).
public record ScheduleEntry(
    String date,          // ISO yyyy-MM-dd (shifts.sdate)
    String dayOfWeek,     // shifts.day_of_week
    String block,         // exact schema block value, e.g. "8:30 Shift"
    boolean surgery,      // shifts.surgery
    List<String> coworkers
) {}

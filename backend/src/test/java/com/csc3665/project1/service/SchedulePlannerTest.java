package com.csc3665.project1.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SchedulePlannerTest {
    private static final LocalDate START = LocalDate.of(2026, 10, 19);
    private static final List<Integer> DOCTORS =
        List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    private static final List<Integer> TECHNICIANS =
        List.of(101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112);

    @Test
    void generatesCoveredBalancedShiftsWithRestAndWeeklyLimits() {
        List<SchedulePlanner.Assignment> assignments = SchedulePlanner.generate(
            START, 14, DOCTORS, TECHNICIANS
        );

        for (int day = 0; day < 14; day++) {
            LocalDate date = START.plusDays(day);
            List<SchedulePlanner.Assignment> daily = assignments.stream()
                .filter(assignment -> assignment.date().equals(date))
                .toList();
            List<String> requiredBlocks = date.getDayOfWeek() == DayOfWeek.SUNDAY
                ? List.of("Sunday Shift", "Overnight")
                : List.of("Early Morning", "8:30 Shift", "Late Doctor", "Overnight");

            for (String block : requiredBlocks) {
                List<SchedulePlanner.Assignment> onShift = daily.stream()
                    .filter(assignment -> assignment.block().equals(block))
                    .toList();
                long doctorCount = onShift.stream()
                    .filter(assignment -> DOCTORS.contains(assignment.employeeId()))
                    .count();
                long technicianCount = onShift.stream()
                    .filter(assignment -> TECHNICIANS.contains(assignment.employeeId()))
                    .count();
                assertTrue(doctorCount >= 1, block + " has a doctor on " + date);
                if (block.equals("Sunday Shift") || block.equals("Overnight")) {
                    assertEquals(1, doctorCount);
                }
                assertEquals(
                    block.equals("Overnight") ? 1 : block.equals("Sunday Shift") ? 2 : doctorCount,
                    technicianCount
                );
                if (block.equals("Early Morning")) {
                    assertTrue(onShift.stream().allMatch(SchedulePlanner.Assignment::surgery));
                }
            }
        }

        Map<Integer, List<SchedulePlanner.Assignment>> byEmployee = assignments.stream()
            .collect(Collectors.groupingBy(SchedulePlanner.Assignment::employeeId));
        for (List<SchedulePlanner.Assignment> employeeShifts : byEmployee.values()) {
            List<SchedulePlanner.Assignment> sorted = employeeShifts.stream()
                .sorted((left, right) -> Long.compare(left.startMinute(), right.startMinute()))
                .toList();
            for (int i = 1; i < sorted.size(); i++) {
                assertTrue(
                    sorted.get(i).startMinute() - sorted.get(i - 1).endMinute() >= 600,
                    "Assignments must have at least 10 hours of rest."
                );
            }
        }

        for (int offset = 0; offset < 14; offset++) {
            LocalDate week = START.plusDays(offset)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            for (int doctor : DOCTORS) {
                List<SchedulePlanner.Assignment> weeklyShifts = assignments.stream()
                    .filter(assignment -> assignment.employeeId() == doctor
                        && assignment.date().with(
                            TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
                        ).equals(week))
                    .toList();
                assertTrue(weeklyShifts.stream().filter(SchedulePlanner.Assignment::surgery).count() <= 1);
                assertTrue(weeklyShifts.stream()
                    .filter(assignment -> assignment.block().equals("Overnight")).count() <= 1);
            }
        }

        assertTrue(countDoctors(assignments, START.with(
            TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)
        ), "Early Morning") >= 2);
        assertTrue(countDoctors(assignments, START.with(
            TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)
        ), "Late Doctor") >= 2);
        assertFalse(assignments.isEmpty());
    }

    @Test
    void rejectsStaffingThatCannotCoverEachNightInAWeek() {
        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> SchedulePlanner.generate(
                START, 7, List.of(1, 2, 3, 4, 5, 6), TECHNICIANS
            )
        );

        assertTrue(error.getMessage().contains("Not enough doctors"));
    }

    @Test
    void respectsRestFromAssignmentsJustOutsideTheGeneratedWindow() {
        LocalDate priorDay = START.minusDays(1);
        SchedulePlanner.Assignment existingNight = SchedulePlanner.existingAssignment(
            DOCTORS.get(0), priorDay, "Overnight", false
        );

        List<SchedulePlanner.Assignment> assignments = SchedulePlanner.generate(
            START, 7, DOCTORS, TECHNICIANS, List.of(existingNight)
        );

        assertFalse(assignments.stream().anyMatch(assignment ->
            assignment.employeeId() == DOCTORS.get(0)
                && assignment.date().equals(START)
                && !assignment.block().equals("Overnight")
        ));
    }

    private long countDoctors(
        List<SchedulePlanner.Assignment> assignments,
        LocalDate date,
        String block
    ) {
        return assignments.stream()
            .filter(assignment -> assignment.date().equals(date)
                && assignment.block().equals(block)
                && DOCTORS.contains(assignment.employeeId()))
            .count();
    }
}
